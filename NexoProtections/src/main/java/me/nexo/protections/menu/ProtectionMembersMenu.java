package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.user.UserManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🛡️ NexoProtections - Menú de Acólitos (Arquitectura Enterprise)
 * Rendimiento: Renderizado Asíncrono por Hilos Virtuales, Lectura RAM O(1) y Dependencias Inyectadas.
 * Nota: Al ser una GUI transitoria, NO lleva @Singleton.
 */
public class ProtectionMembersMenu extends NexoMenu {

    private final NexoProtections plugin;
    private final ProtectionStone stone;

    // 🌟 Sinergia inyectada desde la fábrica
    private final ConfigManager configManager;
    private final ClaimManager claimManager;
    private final CrossplayUtils crossplayUtils;
    private final UserManager userManager;

    // 🌟 OPTIMIZACIÓN O(1): Cacheamos la llave en RAM
    private final NamespacedKey uuidKey;

    // 🌟 FIX: Gestor formal de Hilos Virtuales para no bloquear el TPS
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ProtectionMembersMenu(Player player, ProtectionStone stone, NexoProtections plugin,
                                 ConfigManager configManager, ClaimManager claimManager,
                                 CrossplayUtils crossplayUtils, UserManager userManager) {
        super(player, crossplayUtils); // 🌟 FIX ERROR SUPER: Agregado crossplayUtils
        this.plugin = plugin;
        this.stone = stone;
        this.configManager = configManager;
        this.claimManager = claimManager;
        this.crossplayUtils = crossplayUtils;
        this.userManager = userManager;

        this.uuidKey = new NamespacedKey(plugin, "acolyte_uuid");
    }

    @Override
    public String getMenuName() {
        return configManager.getMessages().menus().miembros().titulo();
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // Pintamos los botones estáticos de inmediato (Síncrono)
        setItem(getSlots() - 6, Material.ENDER_PEARL,
                configManager.getMessages().menus().miembros().items().volver().nombre(), null);

        setItem(getSlots() - 4, Material.WRITABLE_BOOK,
                configManager.getMessages().menus().miembros().items().invocar().nombre(),
                configManager.getMessages().menus().miembros().items().invocar().lore());

        // 🌟 FIX ZERO-LAG: Calculamos los perfiles y metadatos en un Hilo Virtual
        virtualExecutor.submit(() -> {

            // Creamos una lista temporal de ítems ensamblados fuera del hilo principal
            List<ItemStack> headsToLoad = new ArrayList<>();

            for (UUID uuid : stone.getTrustedFriends()) {
                if (headsToLoad.size() >= getSlots() - 9) break;

                // 🌟 FIX ERROR GETNAME: Hacemos todo seguro de forma asíncrona.
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(uuid);
                String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Alma Desconocida";

                var head = new ItemStack(Material.PLAYER_HEAD);
                var meta = (SkullMeta) head.getItemMeta();

                if (meta != null) {
                    meta.setOwningPlayer(targetPlayer);

                    // 🌟 FIX TEXTOS: Adiós al viejo LegacyComponentSerializer
                    String rawHeadName = configManager.getMessages().menus().miembros().items().cabeza().nombre().replace("%player%", targetName);
                    meta.displayName(crossplayUtils.parseCrossplay(player, rawHeadName));

                    List<net.kyori.adventure.text.Component> parsedLore = new ArrayList<>();
                    for (String line : configManager.getMessages().menus().miembros().items().cabeza().lore()) {
                        parsedLore.add(crossplayUtils.parseCrossplay(player, line));
                    }
                    meta.lore(parsedLore);

                    // 🌟 MAGIA PDC CACHEADA: Guardamos el UUID en NBT
                    meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, uuid.toString());
                    head.setItemMeta(meta);
                }
                headsToLoad.add(head);
            }

            // 🌟 VOLVEMOS AL HILO PRINCIPAL: Bukkit requiere que el inventario se modifique sincrónicamente
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Protección Anti-Crash
                if (player.getOpenInventory().getTopInventory() != inventory) return;

                int slot = 0;
                for (ItemStack head : headsToLoad) {
                    inventory.setItem(slot++, head);
                }
            });
        });
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // 🛑 Bloqueo absoluto contra robo de ítems

        var clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Clic en Volver
        if (clicked.getType() == Material.ENDER_PEARL) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

            // 🌟 FIX CRÍTICO: Añadimos 'plugin' a la firma para que coincida con el constructor
            new ProtectionMenu(player, stone, plugin, configManager, claimManager, crossplayUtils, userManager).open();
            return;
        }

        // Clic en Cabeza (Desterrar)
        var meta = clicked.getItemMeta();

        if (clicked.getType() == Material.PLAYER_HEAD && meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {

            // Seguridad: Solo el dueño puede desterrar
            if (!stone.getOwnerId().equals(player.getUniqueId())) return;

            String targetUuidStr = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);

            try {
                UUID targetUuid = UUID.fromString(targetUuidStr);
                stone.removeFriend(targetUuid);

                // 🛡️ FIX DESACOPLADO: Guardado directo usando nuestro ClaimManager inyectado
                claimManager.saveStoneDataAsync(stone);

                crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().destierro());
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

                // Refresco visual O(1)
                inventory.clear();
                setMenuItems();

            } catch (IllegalArgumentException ignored) {}
        }
    }
}
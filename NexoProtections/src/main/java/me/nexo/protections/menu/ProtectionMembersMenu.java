package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ProtectionStone;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🛡️ NexoProtections - Menú de Acólitos (Arquitectura Enterprise)
 * Rendimiento: Cero Lag I/O en bucles, Llaves cacheadas O(1) y Guardado Asíncrono.
 */
public class ProtectionMembersMenu extends NexoMenu {

    private final ProtectionStone stone;
    private final NexoProtections plugin;
    private final ConfigManager configManager;

    // 🌟 OPTIMIZACIÓN O(1): Cacheamos la llave en RAM
    private final NamespacedKey uuidKey;

    public ProtectionMembersMenu(Player player, NexoProtections plugin, ProtectionStone stone) {
        super(player);
        this.plugin = plugin;
        this.stone = stone;
        this.configManager = plugin.getConfigManager();

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

        int slot = 0;
        for (UUID uuid : stone.getTrustedFriends()) {
            if (slot >= getSlots() - 9) break; // Protegemos los botones inferiores

            // 🌟 FIX I/O: Hacemos una sola petición a Bukkit para obtener la Skin y el Nombre al mismo tiempo
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(uuid);
            String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Alma Desconocida";

            // Ensamblado Nativo de alto rendimiento
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                // Asignamos la textura de la cabeza
                meta.setOwningPlayer(targetPlayer);

                String rawHeadName = configManager.getMessages().menus().miembros().items().cabeza().nombre().replace("%player%", targetName);
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(rawHeadName));

                List<net.kyori.adventure.text.Component> parsedLore = new ArrayList<>();
                for (String line : configManager.getMessages().menus().miembros().items().cabeza().lore()) {
                    parsedLore.add(LegacyComponentSerializer.legacySection().deserialize(line));
                }

                meta.lore(parsedLore);

                // 🌟 MAGIA PDC CACHEADA: Guardamos el UUID sin generar basura en la memoria
                meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, uuid.toString());

                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            slot++;
        }

        // Botón Volver
        setItem(getSlots() - 6, Material.ENDER_PEARL,
                configManager.getMessages().menus().miembros().items().volver().nombre(), null);

        // Botón Invocar (Información)
        setItem(getSlots() - 4, Material.WRITABLE_BOOK,
                configManager.getMessages().menus().miembros().items().invocar().nombre(),
                configManager.getMessages().menus().miembros().items().invocar().lore());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // 🛑 Bloqueo absoluto contra robo de ítems

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Clic en Volver
        if (clicked.getType() == Material.ENDER_PEARL) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            new ProtectionMenu(player, plugin, stone).open();
            return;
        }

        // Clic en Cabeza (Desterrar)
        ItemMeta meta = clicked.getItemMeta();

        if (clicked.getType() == Material.PLAYER_HEAD && meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {

            // Seguridad: Solo el dueño puede desterrar
            if (!stone.getOwnerId().equals(player.getUniqueId())) return;

            String targetUuidStr = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);

            try {
                UUID targetUuid = UUID.fromString(targetUuidStr);
                stone.removeFriend(targetUuid);

                // 🛡️ FIX DESACOPLADO: Guardado directo usando nuestro ClaimManager (Asíncrono real)
                plugin.getClaimManager().saveStoneDataAsync(stone);

                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().destierro());
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);

                // Refresco visual O(1)
                inventory.clear();
                setMenuItems();

            } catch (IllegalArgumentException ignored) {}
        }
    }
}
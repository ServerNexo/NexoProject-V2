package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.ArrayList;

/**
 * 🛡️ NexoProtections - Menú Principal del Monolito (Arquitectura Enterprise)
 * Rendimiento: Cero I/O en visualización, Transacciones de Inventario Seguras (Anti-Dupe).
 */
public class ProtectionMenu extends NexoMenu {

    private final ProtectionStone stone;
    private final NexoProtections plugin;
    private final ConfigManager configManager;

    // 🌟 Caché de RAM para no recalcular en cada clic
    private final String cachedOwnerName;

    public ProtectionMenu(Player player, NexoProtections plugin, ProtectionStone stone) {
        super(player);
        this.plugin = plugin;
        this.stone = stone;
        this.configManager = plugin.getConfigManager();

        // 🌟 FIX I/O: Leemos el nombre una sola vez al abrir el menú y lo guardamos en RAM.
        // Evitamos poner Bukkit.getOfflinePlayer() dentro de setMenuItems() para no congelar el TPS.
        String tempName = Bukkit.getOfflinePlayer(stone.getOwnerId()).getName();
        this.cachedOwnerName = (tempName != null) ? tempName : "Desconocido";
    }

    @Override
    public String getMenuName() {
        return configManager.getMessages().menus().principal().titulo();
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🌟 SLOT 11: ACÓLITOS
        setItem(11, Material.WITHER_SKELETON_SKULL,
                configManager.getMessages().menus().principal().acolitos().nombre(),
                configManager.getMessages().menus().principal().acolitos().lore());

        // 🌟 SLOT 13: NÚCLEO MATEMÁTICO (O(1))
        double porcentaje = (stone.getCurrentEnergy() / stone.getMaxEnergy()) * 100;
        String colorEnergia = porcentaje > 50 ? "&#00f5ff" : (porcentaje > 20 ? "&#FFAA00" : "&#FF5555");

        // 🌟 FIX RENDIMIENTO: Cero lambdas innecesarios en un menú que se repinta rápido
        List<String> rawLore = configManager.getMessages().menus().principal().nucleo().lore();
        List<String> parsedLore = new ArrayList<>(rawLore.size());

        for (String line : rawLore) {
            parsedLore.add(line.replace("%owner%", cachedOwnerName)
                    .replace("%type%", stone.getClanId() == null ? "Solitario" : "Sindicato")
                    .replace("%energy_color%", colorEnergia)
                    .replace("%current_energy%", String.format("%.1f", stone.getCurrentEnergy()))
                    .replace("%max_energy%", String.valueOf(stone.getMaxEnergy())));
        }

        setItem(13, Material.LODESTONE, configManager.getMessages().menus().principal().nucleo().nombre(), parsedLore);

        // 🌟 SLOT 15: LEYES (FLAGS)
        setItem(15, Material.SOUL_TORCH,
                configManager.getMessages().menus().principal().leyes().nombre(),
                configManager.getMessages().menus().principal().leyes().lore());

        // 🌟 SLOT 22: RECARGA
        setItem(22, Material.ECHO_SHARD,
                configManager.getMessages().menus().principal().recarga().nombre(),
                configManager.getMessages().menus().principal().recarga().lore());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // 🛑 Prevención absoluta de robo de ítems GUI
        int slot = event.getRawSlot();

        if (slot == 11) { // Acólitos
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            new ProtectionMembersMenu(player, plugin, stone).open();

        } else if (slot == 15) { // Leyes
            if (!stone.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexoprotections.admin")) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().soloDueno());
                return;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1f);
            new ProtectionFlagsMenu(player, plugin, stone).open();

        } else if (slot == 22) { // Recarga
            if (stone.getCurrentEnergy() >= stone.getMaxEnergy()) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().monolitoLleno());
                return;
            }

            // 🌟 BLOQUEO TRANSACCIONAL (ANTI-DUPE Y ROBO)
            if (sacrificarOfrenda(player, Material.ECHO_SHARD, 1)) {
                stone.addEnergy(500);
                recargaExitosa();
            }
            else if (sacrificarOfrenda(player, Material.DIAMOND, 1)) {
                stone.addEnergy(100);
                recargaExitosa();
            }
            else {
                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().ofrendaRechazada());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private void recargaExitosa() {
        CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().recargaExitosa());
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.5f);

        // 🌟 Guardado asíncrono para evitar pérdida de progreso si el server se reinicia
        plugin.getClaimManager().saveStoneDataAsync(stone);

        setMenuItems(); // O(1) Repintado ultrarrápido
    }

    /**
     * 🌟 Transacción Atómica: Verifica y cobra el ítem en la misma operación usando la API de Bukkit.
     * Evita robos matemáticos y falsos positivos de inventario.
     */
    private boolean sacrificarOfrenda(Player player, Material mat, int cantidad) {
        if (!player.getInventory().containsAtLeast(new ItemStack(mat), cantidad)) {
            return false;
        }

        // removeItem elimina exactamente la cantidad requerida, buscando en todos los stacks del inventario
        player.getInventory().removeItem(new ItemStack(mat, cantidad));
        return true;
    }
}
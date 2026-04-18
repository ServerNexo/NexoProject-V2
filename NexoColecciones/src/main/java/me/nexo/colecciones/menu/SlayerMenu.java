package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 📚 NexoColecciones - Menú de Contratos Slayer (Arquitectura Enterprise)
 * Rendimiento: Cero Lag Visual (0 I/O), Llaves Cacheadas y Renderizado O(1).
 */
public class SlayerMenu extends NexoMenu {

    private final NexoColecciones plugin;

    // 🌟 OPTIMIZACIÓN DE RAM: Llaves de PDC cacheadas
    private final NamespacedKey actionKey;
    private final NamespacedKey slayerKey;

    public SlayerMenu(Player player, NexoColecciones plugin) {
        super(player);
        this.plugin = plugin;

        // Cacheamos las llaves una sola vez al abrir el menú
        this.actionKey = new NamespacedKey(plugin, "action");
        this.slayerKey = new NamespacedKey(plugin, "slayer_id");
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Texto Hexadecimal directo (0% Lag I/O)
        return "&#FF5555⚔ <bold>CONTRATOS SLAYER</bold>";
    }

    @Override
    public int getSlots() {
        return 27; // Inventario de 3 filas
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Rellena los huecos vacíos con cristal morado del Vacío

        SlayerManager manager = plugin.getSlayerManager();
        int slot = 10; // Empezamos a colocar los jefes en el centro de la interfaz

        // Leemos desde el SlayerTemplate almacenado en RAM
        for (SlayerManager.SlayerTemplate template : manager.getTemplates().values()) {
            if (slot >= 17) break; // Límite de seguridad visual (1 fila central)

            Material mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>" + template.name().toUpperCase() + "</bold>"));

                // 🌟 FIX: Lore directo, estático y cacheado. Cero tirones al abrir el menú.
                List<net.kyori.adventure.text.Component> lore = List.of(
                        CrossplayUtils.parseCrossplay(player, "&#555555Contrato de Exterminio"),
                        CrossplayUtils.parseCrossplay(player, ""),
                        CrossplayUtils.parseCrossplay(player, "&#E6CCFFObjetivo: &#FF5555" + template.targetMob()),
                        CrossplayUtils.parseCrossplay(player, "&#E6CCFFKills Requeridas: &#FFAA00" + template.requiredKills()),
                        CrossplayUtils.parseCrossplay(player, "&#E6CCFFJefe a Invocar: &#55FF55" + template.bossName()),
                        CrossplayUtils.parseCrossplay(player, ""),
                        CrossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para iniciar el contrato")
                );

                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

                // Asignamos las llaves de caché
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "start_slayer");
                meta.getPersistentDataContainer().set(slayerKey, PersistentDataType.STRING, template.id());

                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        // Validamos usando la llave cacheada
        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if ("start_slayer".equals(action)) {
                String slayerId = meta.getPersistentDataContainer().get(slayerKey, PersistentDataType.STRING);

                player.closeInventory();

                // Iniciamos la cacería interactuando con el Manager
                plugin.getSlayerManager().iniciarSlayer(player, slayerId);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            }
        }
    }
}
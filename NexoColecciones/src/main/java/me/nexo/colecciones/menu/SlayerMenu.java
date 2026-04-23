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
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 📚 NexoColecciones - Menú de Contratos Slayer (Arquitectura Enterprise)
 * Rendimiento: Cero Lag Visual (0 I/O), Llaves Cacheadas y Dependencias Inyectadas.
 * Nota: Al ser una GUI transitoria (1 por jugador), NO lleva @Singleton.
 */
public class SlayerMenu extends NexoMenu {

    private final NexoColecciones plugin;
    
    // 🌟 Sinergia inyectada desde la fábrica
    private final SlayerManager slayerManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 OPTIMIZACIÓN DE RAM: Llaves de PDC cacheadas
    private final NamespacedKey actionKey;
    private final NamespacedKey slayerKey;

    public SlayerMenu(Player player, NexoColecciones plugin, SlayerManager slayerManager, CrossplayUtils crossplayUtils) {
        super(player);
        this.plugin = plugin;
        this.slayerManager = slayerManager;
        this.crossplayUtils = crossplayUtils;

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

        int slot = 10; // Empezamos a colocar los jefes en el centro de la interfaz

        // 🌟 FIX: Leemos desde el SlayerTemplate inyectado en RAM (Cero Service Locators)
        for (var template : slayerManager.getTemplates().values()) {
            if (slot >= 17) break; // Límite de seguridad visual (1 fila central)

            var mat = Material.matchMaterial(template.targetMob() + "_SPAWN_EGG");
            if (mat == null) mat = Material.SKELETON_SKULL;

            var item = new ItemStack(mat);
            var meta = item.getItemMeta();
            
            if (meta != null) {
                // 🌟 FIX: Instancia inyectada para parseo de colores
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>" + template.name().toUpperCase() + "</bold>"));

                // Lore directo, estático y cacheado. Cero tirones al abrir el menú.
                List<net.kyori.adventure.text.Component> lore = List.of(
                        crossplayUtils.parseCrossplay(player, "&#555555Contrato de Exterminio"),
                        crossplayUtils.parseCrossplay(player, ""),
                        crossplayUtils.parseCrossplay(player, "&#E6CCFFObjetivo: &#FF5555" + template.targetMob()),
                        crossplayUtils.parseCrossplay(player, "&#E6CCFFKills Requeridas: &#FFAA00" + template.requiredKills()),
                        crossplayUtils.parseCrossplay(player, "&#E6CCFFJefe a Invocar: &#55FF55" + template.bossName()),
                        crossplayUtils.parseCrossplay(player, ""),
                        crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para iniciar el contrato")
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

        var item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();

        // Validamos usando la llave cacheada
        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if ("start_slayer".equals(action)) {
                String slayerId = meta.getPersistentDataContainer().get(slayerKey, PersistentDataType.STRING);

                player.closeInventory();

                // 🌟 FIX: Iniciamos la cacería interactuando con el Manager Inyectado
                slayerManager.iniciarSlayer(player, slayerId);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            }
        }
    }
}
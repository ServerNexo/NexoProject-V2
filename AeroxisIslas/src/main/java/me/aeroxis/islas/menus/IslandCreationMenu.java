package me.aeroxis.islas.menus;

import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.islas.AeroxisIslas;
import me.aeroxis.islas.data.IslandDatabase;
import me.aeroxis.islas.managers.IslandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏝️ Menú de Selección de Plantillas
 * UX limpia para la creación inicial del imperio.
 */
public class IslandCreationMenu extends AeroxisMenu {

    private final AeroxisIslas plugin;
    private final IslandManager islandManager;
    private final IslandDatabase db;

    public IslandCreationMenu(Player player, CrossplayUtils crossplayUtils, AeroxisIslas plugin, IslandManager islandManager, IslandDatabase db) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.db = db;
    }

    @Override
    public String getMenuName() {
        return "&#00f5ff✨ &#55FF55Selecciona tu Bioma";
    }

    @Override
    public int getSlots() {
        return 27; // 3 Filas es suficiente para esto
    }

    @Override
    public void setMenuItems() {
        // 🔲 FONDO INMERSIVO
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        bg.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) inventory.setItem(i, bg);

        // 🌳 ISLA CLÁSICA (Slot 11)
        ItemStack normal = new ItemStack(Material.GRASS_BLOCK);
        normal.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55<bold>🌳 Isla Clásica</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAEl desafío Skyblock original."));
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAARecursos balanceados para empezar."));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para materializar"));
            meta.lore(lore);
        });
        inventory.setItem(11, normal);

        // 🌵 ISLA DESÉRTICA (Slot 13)
        ItemStack desert = new ItemStack(Material.SAND);
        desert.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00<bold>🌵 Isla Desértica</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAUn entorno hostil y caluroso."));
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAIdeal para granjas de cactus y arena."));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para materializar"));
            meta.lore(lore);
        });
        inventory.setItem(13, desert);

        // ❄️ ISLA HELADA (Slot 15)
        ItemStack ice = new ItemStack(Material.BLUE_ICE);
        ice.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff<bold>❄️ Isla Helada</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAATemperaturas bajo cero."));
            lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAARecursos congelados y nieve infinita."));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para materializar"));
            meta.lore(lore);
        });
        inventory.setItem(15, ice);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        String templateName = null;

        switch (e.getSlot()) {
            case 11: templateName = "island_template"; break;
            case 13: templateName = "island_desert"; break; // Nombres de los mundos plantilla en tu carpeta slime_worlds
            case 15: templateName = "island_ice"; break;
        }

        if (templateName != null) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1f, 2f);
            crossplayUtils.sendMessage(player, "&#FFAA00⏳ Firmando escrituras y materializando el núcleo...");

            // 🌟 FLUJO ASÍNCRONO DE CREACIÓN
            // En el futuro, pasaremos el 'templateName' a generatePhysicalIsland
            db.createNewIsland(player.getUniqueId(), "Isla de " + player.getName()).thenAccept(newProfile -> {
                if (newProfile != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // TODO: Ajustaremos islandManager para que reciba 'templateName'
                        islandManager.generatePhysicalIsland(player, newProfile);
                    });
                } else {
                    crossplayUtils.sendMessage(player, "&#FF5555[x] Error crítico con el Nexo Central (BD).");
                }
            });
        }
    }
}
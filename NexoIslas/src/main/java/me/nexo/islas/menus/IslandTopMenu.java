package me.nexo.islas.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.islas.data.IslandDatabase;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 🏆 Salón de la Fama - Top de Islas
 * Rendimiento: Carga asíncrona pre-calculada, UI 54 slots y renderizado de cabezas.
 */
public class IslandTopMenu extends NexoMenu {

    private final List<IslandDatabase.IslandTopEntry> topLevel;
    private final List<IslandDatabase.IslandTopEntry> topValue;

    public IslandTopMenu(Player player, CrossplayUtils crossplayUtils, 
                         List<IslandDatabase.IslandTopEntry> topLevel, 
                         List<IslandDatabase.IslandTopEntry> topValue) {
        super(player, crossplayUtils);
        this.topLevel = topLevel;
        this.topValue = topValue;
    }

    @Override
    public String getMenuName() {
        return "&#FFD700🏆 <bold>Salón de la Fama</bold>";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        // 🔲 FONDO INMERSIVO
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        bg.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) inventory.setItem(i, bg);

        // ⚔️ SEPARADOR CENTRAL (Columna 4)
        ItemStack separator = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        separator.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00<bold>NEXO</bold>")));
        for (int i = 4; i < 54; i += 9) inventory.setItem(i, separator);

        // 🏷️ TÍTULOS DE COLUMNAS
        ItemStack titleLevel = new ItemStack(Material.EXPERIENCE_BOTTLE);
        titleLevel.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff<bold>📈 TOP POR NIVEL (Actividad)</bold>")));
        inventory.setItem(2, titleLevel);

        ItemStack titleValue = new ItemStack(Material.EMERALD);
        titleValue.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55<bold>💎 TOP POR VALOR (Riqueza)</bold>")));
        inventory.setItem(6, titleValue);

        // 🌟 POSICIONES PARA EL TOP DE NIVEL (Izquierda)
        int[] levelSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37};
        renderTop(topLevel, levelSlots, "&#00f5ffNivel de Isla: &#FFFFFF", "Nivel");

        // 🌟 POSICIONES PARA EL TOP DE VALOR (Derecha)
        int[] valueSlots = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41};
        renderTop(topValue, valueSlots, "&#55FF55Cristales de Valor: &#FFFFFF", "Valor");

        // ❌ BOTÓN DE CERRAR
        ItemStack close = new ItemStack(Material.BARRIER);
        close.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555<bold>Cerrar Menú</bold>")));
        inventory.setItem(49, close);
    }

    private void renderTop(List<IslandDatabase.IslandTopEntry> list, int[] slots, String scorePrefix, String type) {
        for (int i = 0; i < slots.length; i++) {
            if (i < list.size()) {
                IslandDatabase.IslandTopEntry entry = list.get(i);
                
                // Colores para el Podio (Top 1, 2, 3)
                String positionColor = switch (i) {
                    case 0 -> "&#FFD700<bold>#1</bold> "; // Oro
                    case 1 -> "&#AAAAAA<bold>#2</bold> "; // Plata
                    case 2 -> "&#FF5555<bold>#3</bold> "; // Bronce
                    default -> "&#555555#" + (i + 1) + " ";
                };

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                head.editMeta(meta -> {
                    if (meta instanceof SkullMeta skull) {
                        try {
                            OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(entry.ownerName())); // Asumimos que ownerName guarda el UUID en tu DB
                            skull.setOwningPlayer(p);
                        } catch (IllegalArgumentException ignored) {}

                        skull.displayName(crossplayUtils.parseCrossplay(player, positionColor + "&#FFFFFF" + entry.islandName()));
                        
                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(crossplayUtils.parseCrossplay(player, scorePrefix + entry.score()));
                        lore.add(Component.empty());
                        lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFLos reyes del " + type + " del servidor."));
                        skull.lore(lore);
                    }
                });
                inventory.setItem(slots[i], head);
            } else {
                // Espacio Vacío si no hay 10 islas
                ItemStack empty = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                empty.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#AAAAAA<bold>Puesto Vacante</bold>")));
                inventory.setItem(slots[i], empty);
            }
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        if (e.getSlot() == 49) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1f);
            player.closeInventory();
        }
    }
}
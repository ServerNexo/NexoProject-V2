package me.aeroxis.chat.menu;

import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.AeroxisMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class AeroxisViewerMenu {

    /**
     * Abre un GUI de "Solo Lectura" con el inventario o EnderChest de un jugador.
     * Renderizado nativo con AeroxisMenu (cero dependencias externas).
     */
    public void open(Player viewer, Player target, String type) {
        boolean isEnderChest = type.equalsIgnoreCase("ec");
        String title = isEnderChest ? "§8☁ EnderChest de " + target.getName() : "§8🎒 Inventario de " + target.getName();
        int rows = isEnderChest ? 3 : 5; // El EC tiene 3 filas (27), el Inv tiene 4 (36) + armadura

        // 🌟 Extraemos la utilidad del Core en tiempo de ejecución para no romper el comando actual
        CrossplayUtils crossplayUtils = JavaPlugin.getPlugin(AeroxisCore.class).getInjector().getInstance(CrossplayUtils.class);

        // 🌟 Creamos el Menú al vuelo (Clase Anónima)
        AeroxisMenu gui = new AeroxisMenu(viewer, crossplayUtils) {
            @Override
            public String getMenuName() {
                return title;
            }

            @Override
            public int getSlots() {
                return rows * 9;
            }

            @Override
            public void setMenuItems() {
                ItemStack[] items = isEnderChest ? target.getEnderChest().getContents() : target.getInventory().getContents();

                for (int i = 0; i < items.length; i++) {
                    ItemStack item = items[i];
                    if (item != null && item.getType() != Material.AIR) {

                        // Si es el inventario normal, mapeamos la armadura a la última fila para que se vea ordenado
                        int slot = i;
                        if (!isEnderChest && i >= 36) { // Slots 36-39 son armadura, 40 es la mano secundaria
                            slot = (i - 36) + 36; // Lo colocamos en la fila de abajo
                        }

                        // Asegurarnos de no salirnos del límite del GUI
                        if (slot < getSlots()) {
                            // 🌟 Usamos el inventario nativo para preservar todos los encantamientos/NBTs del ítem original
                            inventory.setItem(slot, item);
                        }
                    }
                }
            }

            @Override
            public void handleMenu(InventoryClickEvent e) {
                // 🛡️ MAGIA AAA: Bloquea clicks, arrastres y shift-clicks. ¡Cero robos/dupeos!
                e.setCancelled(true);
            }
        };

        gui.open();
    }
}
package me.nexo.chat.menu;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NexoViewerMenu {

    /**
     * Abre un GUI de "Solo Lectura" con el inventario o EnderChest de un jugador.
     */
    public void open(Player viewer, Player target, String type) {
        boolean isEnderChest = type.equalsIgnoreCase("ec");
        String title = isEnderChest ? "☁ EnderChest de " + target.getName() : "🎒 Inventario de " + target.getName();
        int rows = isEnderChest ? 3 : 5; // El EC tiene 3 filas (27), el Inv tiene 4 (36) + armadura

        // Creamos el GUI de Triumph-GUI
        Gui gui = Gui.gui()
                .title(Component.text(title))
                .rows(rows)
                .disableAllInteractions() // 🛡️ MAGIA AAA: Bloquea clicks, arrastres y shift-clicks. ¡Cero robos/dupeos!
                .create();

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
                if (slot < rows * 9) {
                    gui.setItem(slot, new GuiItem(item));
                }
            }
        }

        gui.open(viewer);
    }
}
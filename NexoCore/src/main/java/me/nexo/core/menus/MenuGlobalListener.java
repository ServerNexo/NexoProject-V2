package me.nexo.core.menus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuGlobalListener implements Listener {

    // 🖱️ DETECTAR CLICS EN LOS MENÚS
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        // Obtenemos al dueño del inventario principal (el menú que está arriba)
        InventoryHolder holder = event.getInventory().getHolder();

        // 🌟 LA MAGIA: ¿El inventario es parte de nuestro nuevo sistema NexoMenu?
        if (holder instanceof NexoMenu nexoMenu) {
            // Le pasamos la pelota al menú específico que el jugador tiene abierto
            // Nota: La cancelación del evento (event.setCancelled(true))
            // la maneja cada menú individualmente dentro de su propio handleMenu()
            nexoMenu.handleMenu(event);
        }
    }

    // 🚪 DETECTAR CUANDO SE CIERRAN LOS MENÚS
    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        // Obtenemos al dueño del inventario que se acaba de cerrar
        InventoryHolder holder = event.getInventory().getHolder();

        // Si el inventario es de nuestra Arquitectura Omega, ejecutamos su método de cierre
        if (holder instanceof NexoMenu nexoMenu) {
            nexoMenu.handleClose(event);
        }
    }
}
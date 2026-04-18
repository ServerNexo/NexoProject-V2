package me.nexo.core.menus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 🏛️ Nexo Network - Listener de Menú (Enterprise)
 */
@Singleton
public class VoidBlessingMenuListener implements Listener {

    @Inject
    public VoidBlessingMenuListener() {}

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Bloqueo instantáneo si la interfaz pertenece al VoidBlessingMenu
        if (event.getInventory().getHolder() instanceof VoidBlessingMenu) {
            event.setCancelled(true);
        }
    }
}
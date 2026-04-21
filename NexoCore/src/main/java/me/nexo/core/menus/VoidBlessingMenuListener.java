package me.nexo.core.menus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 🏛️ Nexo Network - Listener de Menú (Arquitectura Enterprise)
 * Validado para Guice: Instancia única para prevenir fugas de memoria en eventos.
 */
@Singleton
public class VoidBlessingMenuListener implements Listener {

    // 💉 PILAR 1: Constructor inyectable para el ecosistema Guice
    @Inject
    public VoidBlessingMenuListener() {
        // Constructor vacío preparado para ser instanciado por el ServiceBootstrap
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // Bloqueo instantáneo de O(1) si la interfaz pertenece al VoidBlessingMenu
        if (event.getInventory().getHolder() instanceof VoidBlessingMenu) {
            event.setCancelled(true);
        }
    }
}
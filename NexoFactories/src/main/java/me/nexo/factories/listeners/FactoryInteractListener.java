package me.nexo.factories.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.managers.FactoryManager;
import me.nexo.factories.menu.FactoryMenu;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 🏭 NexoFactories - Listener de Interacción con Máquinas (Arquitectura Enterprise)
 * Rendimiento: Búsqueda Espacial O(1), Inyección Directa y Blindaje Anti-Robos.
 */
@Singleton
public class FactoryInteractListener implements Listener {

    private final NexoFactories plugin;
    private final FactoryManager factoryManager;

    // 💉 PILAR 3: Inyección de Dependencias Directa
    @Inject
    public FactoryInteractListener(NexoFactories plugin, FactoryManager factoryManager) {
        this.plugin = plugin;
        this.factoryManager = factoryManager; // Inyectado directamente, cero saltos.
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        // 🌟 FIX: Ignoramos la mano secundaria para prevenir el "Double-Fire Bug"
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Solo nos interesa si hace clic derecho en un bloque físico
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // 🚀 Búsqueda Espacial instantánea O(1) gracias a la caché que construimos antes
        ActiveFactory factory = factoryManager.getFactoryAt(clicked.getLocation());

        if (factory != null) {
            event.setCancelled(true); // Cancelamos interactuar con hornos/mesas vanilla
            Player player = event.getPlayer();

            // 🛡️ PARCHE DE SEGURIDAD: Evita el espionaje y robo industrial
            if (!factory.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexofactories.admin")) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Acceso Denegado. Esta maquinaria pertenece a otro jugador.");
                return;
            }

            // 🌟 ABRIMOS LA INTERFAZ MODERNIZADA
            new FactoryMenu(player, plugin, factory).open();
        }
    }
}
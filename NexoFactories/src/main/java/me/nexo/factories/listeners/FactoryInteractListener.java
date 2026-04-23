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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 🏭 NexoFactories - Listener de Interacción con Máquinas (Arquitectura Enterprise Java 21)
 * Rendimiento: Búsqueda Espacial O(1), Inyección Transitiva Directa y Blindaje Anti-Robos.
 */
@Singleton
public class FactoryInteractListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoFactories plugin;
    private final FactoryManager factoryManager;
    private final CrossplayUtils crossplayUtils;
    
    // Caché segura de la Soft-Dependency para pasarla al menú
    private Object claimManagerCache;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public FactoryInteractListener(NexoFactories plugin, FactoryManager factoryManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.factoryManager = factoryManager;
        this.crossplayUtils = crossplayUtils;
        
        // Cacheamos el ClaimManager de forma segura en el inicio para no asfixiar el evento de clic
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("NexoProtections")) {
                Class<?> apiClass = Class.forName("me.nexo.core.user.NexoAPI");
                Object services = apiClass.getMethod("getServices").invoke(null);
                Object optManager = services.getClass().getMethod("get", Class.class).invoke(services, Class.forName("me.nexo.protections.managers.ClaimManager"));
                
                if (optManager instanceof java.util.Optional<?> opt && opt.isPresent()) {
                    this.claimManagerCache = opt.get();
                }
            }
        } catch (Exception ignored) {
            // Falla silenciosa permitida: si no existe, el menú simplemente mostrará "Desconectada"
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        // 🌟 FIX: Ignoramos la mano secundaria para prevenir el "Double-Fire Bug"
        if (event.getHand() != EquipmentSlot.HAND) return;

        // 🌟 MODERNIZACIÓN PAPER: Método isRightClick()
        if (!event.getAction().isRightClick()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // 🚀 Búsqueda Espacial instantánea O(1)
        ActiveFactory factory = factoryManager.getFactoryAt(clicked.getLocation());

        if (factory != null) {
            event.setCancelled(true); // Cancelamos interactuar con hornos/mesas vanilla
            Player player = event.getPlayer();

            // 🛡️ PARCHE DE SEGURIDAD: Evita el espionaje y robo industrial
            if (!factory.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexofactories.admin")) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Acceso Denegado. Esta maquinaria pertenece a otro jugador.");
                return;
            }

            // 🌟 INYECCIÓN TRANSITIVA: Pasamos las dependencias limpias a la interfaz
            new FactoryMenu(player, plugin, factoryManager, crossplayUtils, claimManagerCache, factory).open();
        }
    }
}
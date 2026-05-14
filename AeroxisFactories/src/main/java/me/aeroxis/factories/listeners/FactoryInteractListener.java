package me.aeroxis.factories.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.factories.AeroxisFactories;
import me.aeroxis.factories.core.ActiveFactory;
import me.aeroxis.factories.managers.FactoryManager;
import me.aeroxis.factories.menu.FactoryMenu;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 🏭 AeroxisFactories - Listener de Interacción con Máquinas (Arquitectura Enterprise Java 21)
 * Rendimiento: Búsqueda Espacial O(1), Inyección Transitiva Directa y Blindaje Anti-Robos Bedrock Safe.
 */
@Singleton
public class FactoryInteractListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final AeroxisFactories plugin;
    private final FactoryManager factoryManager;
    private final CrossplayUtils crossplayUtils;

    // Caché segura de la Soft-Dependency para pasarla al menú
    private Object claimManagerCache;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public FactoryInteractListener(AeroxisFactories plugin, FactoryManager factoryManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.factoryManager = factoryManager;
        this.crossplayUtils = crossplayUtils;

        // Cacheamos el ClaimManager de forma segura en el inicio para no asfixiar el evento de clic
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("AeroxisProtections")) {
                Class<?> apiClass = Class.forName("me.aeroxis.core.user.AeroxisAPI");
                Object services = apiClass.getMethod("getServices").invoke(null);
                Object optManager = services.getClass().getMethod("get", Class.class).invoke(services, Class.forName("me.aeroxis.protections.managers.ClaimManager"));

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

    // ==========================================
    // 🗑️ EVENTO: DESMANTELAR FÁBRICA (BEDROCK SAFE)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block clicked = event.getBlock();
        Player player = event.getPlayer();

        // 🌟 Búsqueda Espacial Ampliada (Detecta cualquier bloque de la máquina, no solo el núcleo)
        ActiveFactory factory = factoryManager.getFactoryFromStructuralBlock(clicked.getLocation());

        if (factory != null) {

            // 🛡️ PARCHE DE SEGURIDAD
            if (!factory.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexofactories.admin")) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Acceso Denegado. No puedes desmantelar maquinaria ajena.");
                event.setCancelled(true);
                return;
            }

            // ⚠️ MECÁNICA DE DESMANTELAMIENTO (DOBLE CONFIRMACIÓN BEDROCK)
            if (player.isSneaking()) {
                // Borramos de RAM y de MySQL Asíncronamente
                factoryManager.deleteFactoryAsync(factory);

                // TODO (Opcional): Si tienes un método para dar el BlueprintItem de vuelta, llámalo aquí.
                // Ej: player.getInventory().addItem(BlueprintGenerator.getBlueprint(factory.getFactoryType()));

                crossplayUtils.sendMessage(player, "&#FF5555[!] <bold>FÁBRICA DESMANTELADA:</bold> &#E6CCFFLa maquinaria se ha comprimido en su plano base.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 0.8f);
            } else {
                // El jugador intentó romper un bloque sin agacharse.
                // Se cancela para evitar que rompan la máquina por accidente (Especialmente en Bedrock/Móvil).
                crossplayUtils.sendMessage(player, "&#FFAA00[⚠️] Este bloque pertenece a una fábrica activa.");
                crossplayUtils.sendMessage(player, "&#FFAA00Mantén presionado Agacharse (Sneak) y rómpelo para desmantelarla.");
                event.setCancelled(true);
            }
        }
    }
}
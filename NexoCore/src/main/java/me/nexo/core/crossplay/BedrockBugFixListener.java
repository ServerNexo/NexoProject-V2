package me.nexo.core.crossplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ Nexo Network - Bedrock Bug Fixer (Arquitectura Enterprise)
 * Soluciona desincronizaciones de red, dobles clics en GUIs y desfase de Reach en Bedrock.
 */
@Singleton
public class BedrockBugFixListener implements Listener {

    // 🛡️ Memoria RAM concurrente para el Anti-Double Tap
    private final Map<UUID, Long> guiCooldownMap = new ConcurrentHashMap<>();

    @Inject
    public BedrockBugFixListener() {
        // Constructor inyectado por Guice
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDoubleTap(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 📱 Verificación de Floodgate (API Externa)
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            long lastClick = guiCooldownMap.getOrDefault(player.getUniqueId(), 0L);

            // Cooldown estricto de 200ms para prevenir dobles compras accidentales en móviles
            if (now - lastClick < 200) {
                event.setCancelled(true);
                return;
            }
            guiCooldownMap.put(player.getUniqueId(), now);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedrockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) return;

        // 🛡️ 1. Reach Distance Fix (Equilibrio Java vs Bedrock)
        if (event.getClickedBlock() != null) {
            double distance = player.getEyeLocation().distance(event.getClickedBlock().getLocation());
            if (distance > 4.5) {
                event.setCancelled(true);
                return;
            }
        }

        // 🛡️ 2. Interaction Bridge (Fix para bloques custom o invisibles en clientes móviles)
        if (event.getAction().isRightClick()) {
            // Disparamos un RayTrace de alto rendimiento ignorando fluidos
            RayTraceResult result = player.getWorld().rayTraceBlocks(
                    player.getEyeLocation(),
                    player.getEyeLocation().getDirection(),
                    4.5,
                    FluidCollisionMode.NEVER,
                    true
            );

            if (result != null && result.getHitBlock() != null) {
                Block hit = result.getHitBlock();
                String type = hit.getType().name();

                // Detección de bloques funcionales (Nativo 1.21.5 - Incluye Crafter)
                if (type.contains("FURNACE") ||
                    type.contains("CRAFTER") ||
                    type.contains("DROPPER") ||
                    type.contains("BARREL")  ||
                    type.contains("BEACON")) {

                    // Nota del Arquitecto: En la Fase 3, aquí inyectaremos los managers
                    // de NexoFactories para forzar aperturas de menús asíncronos.
                }
            }
        }
    }

    /**
     * Limpieza de caché para evitar fugas de memoria en reloads.
     */
    public void clearCache() {
        this.guiCooldownMap.clear();
    }
}
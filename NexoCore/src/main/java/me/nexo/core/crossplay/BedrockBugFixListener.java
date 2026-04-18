package me.nexo.core.crossplay;

import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BedrockBugFixListener implements Listener {

    // Memoria RAM de alta velocidad para el Anti-Double Tap
    private final Map<UUID, Long> guiCooldownMap = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDoubleTap(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 🛡️ Solo filtramos a los jugadores de Bedrock (Consola/Móvil)
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            long lastClick = guiCooldownMap.getOrDefault(player.getUniqueId(), 0L);

            // Cooldown estricto de 200ms para prevenir dobles compras accidentales
            if (now - lastClick < 200) {
                event.setCancelled(true);
                return;
            }
            guiCooldownMap.put(player.getUniqueId(), now);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBedrockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) return;

        // 🛡️ 1. Reach Distance Fix (Anula el bug donde Bedrock pega desde muy lejos)
        if (event.getClickedBlock() != null) {
            double distance = player.getEyeLocation().distance(event.getClickedBlock().getLocation());
            if (distance > 4.5) {
                event.setCancelled(true);
                return;
            }
        }

        // 🛡️ 2. Interaction Bridge (Solución al "clic al aire" frente a bloques invisibles o custom)
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            // Disparamos un RayTrace asíncrono-safe ignorando agua/lava
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

                // Si el jugador de móvil miraba a un bloque funcional pero el cliente se bugueó y envió "AIR":
                if (type.contains("FURNACE") || type.contains("CRAFTER") || type.contains("DROPPER") || type.contains("BARREL") || type.contains("BEACON")) {

                    // 🌟 MAGIA: Si tienes la API de NexoFactories, aquí podrías forzar su apertura directa.
                    // Por ejemplo:
                    // if (NexoFactories.getFactoryManager().isFactory(hit)) {
                    //     NexoFactories.getFactoryManager().openMenu(player, hit);
                    // }

                }
            }
        }
    }
}
package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.data.IslandRole;
import me.nexo.islas.managers.IslandManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 🛡️ Seguridad y Permisos AAA (Arquitectura RAM / Grid)
 * Lee la memoria instantáneamente para decidir si cancelar o no un evento sin usar la Base de Datos.
 */
@Singleton
public class IslandSecurityListener implements Listener {

    private final IslandManager islandManager;

    // Configuraciones matemáticas del Grid
    private static final int ISLAND_SPACING = 1000;
    private static final int BLOCKS_PER_LEVEL = 25; // Radio: Nivel 1 = 25x25, Nivel 2 = 50x50...

    @Inject
    public IslandSecurityListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    // ==========================================
    // 🧱 1. PERMISOS DE CONSTRUCCIÓN Y ROTURA
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        handleBlockAction(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockAction(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    private void handleBlockAction(Player player, Location loc, org.bukkit.event.Cancellable event) {
        if (!loc.getWorld().getName().equals("nexo_islas_world")) return;

        // 🌟 BÚSQUEDA INSTANTÁNEA (O(1) en RAM usando matemática inversa)
        IslandProfile profile = islandManager.getIslandAt(loc);

        if (profile == null) {
            // El jugador está intentando construir en el vacío absoluto entre islas
            event.setCancelled(true);
            return;
        }

        IslandRole role = profile.getRole(player.getUniqueId());

        if (role == IslandRole.VISITOR) {
            event.setCancelled(true);
            player.sendMessage("§c❌ Eres un visitante. No tienes permiso para construir o destruir aquí.");
        } else if (!isWithinBorder(loc, profile)) {
            event.setCancelled(true);
            player.sendMessage("§c❌ Has alcanzado el límite de frontera de la isla.");
        }
    }

    // ==========================================
    // 📦 2. PERMISOS DE INTERACCIÓN (Cofres, Hornos, Puertas)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Location loc = event.getClickedBlock().getLocation();
        if (!loc.getWorld().getName().equals("nexo_islas_world")) return;

        IslandProfile profile = islandManager.getIslandAt(loc);
        if (profile == null) return;

        IslandRole role = profile.getRole(event.getPlayer().getUniqueId());

        // Bloqueamos a los visitantes de robar cofres o usar utilidades
        if (role == IslandRole.VISITOR) {
            Material type = event.getClickedBlock().getType();
            if (type.name().contains("CHEST") || type.name().contains("DOOR") || type.name().contains("GATE") ||
                    type == Material.HOPPER || type == Material.FURNACE || type == Material.BARREL || type == Material.DISPENSER) {

                event.setCancelled(true);
                event.getPlayer().sendMessage("§c❌ Los visitantes no pueden interactuar con esto.");
            }
        }
    }

    // ==========================================
    // 🚶 3. FRONTERA DE MOVIMIENTO (Anti-Escape)
    // ==========================================
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Evitamos lag: Solo verificamos si el jugador cambió de bloque entero (Ignora mover la cámara)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals("nexo_islas_world")) return;

        // Obtenemos la isla de la coordenada a la que intenta pisar
        IslandProfile profile = islandManager.getIslandAt(event.getTo());

        if (profile == null) {
            // Intenta caminar hacia el vacío absoluto fuera del grid de cualquier isla
            player.teleport(event.getFrom());
        } else {
            // Si la isla está bloqueada (candado) y el jugador es solo un visitante, lo rebotamos
            if (profile.isLocked() && profile.getRole(player.getUniqueId()) == IslandRole.VISITOR) {
                player.teleport(event.getFrom());
                player.sendMessage("§c❌ Esta isla está cerrada para visitantes.");
            }
            // Si intenta pasar los límites de la frontera (borde invisible)
            else if (!isWithinBorder(event.getTo(), profile)) {
                player.teleport(event.getFrom());
            }
        }
    }

    // ==========================================
    // 🧮 FÓRMULAS MATEMÁTICAS DEL GRID
    // ==========================================
    private boolean isWithinBorder(Location loc, IslandProfile profile) {
        // Calculamos el centro exacto de la isla
        int centerX = (profile.getGridIndex() % 100) * ISLAND_SPACING;
        int centerZ = (profile.getGridIndex() / 100) * ISLAND_SPACING;

        // Calculamos el radio permitido según el nivel de frontera
        int radius = profile.getBorderLevel() * BLOCKS_PER_LEVEL;

        // Comparamos si la distancia X o Z superan el radio permitido
        return Math.abs(loc.getBlockX() - centerX) <= radius && Math.abs(loc.getBlockZ() - centerZ) <= radius;
    }
}
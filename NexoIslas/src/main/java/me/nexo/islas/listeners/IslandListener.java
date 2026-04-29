package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.managers.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

@Singleton
public class IslandListener implements Listener {

    private final NexoIslas plugin;
    private final IslandManager islandManager;

    @Inject
    public IslandListener(NexoIslas plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin); // Auto-registro
    }

    // 1. Cuando el jugador se desconecta del servidor
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleIslandUnload(event.getPlayer().getWorld());
    }

    // 2. Cuando el jugador usa /spawn o se va a otro mundo
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        handleIslandUnload(event.getFrom());
    }

    private void handleIslandUnload(World world) {
        // Verificamos si el mundo es un micromundo Slime de isla
        if (world.getName().startsWith("island_")) {
            // Esperamos 2 ticks para asegurar que el jugador ya no está físicamente
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (world.getPlayers().isEmpty()) {
                    try {
                        // Extraemos el UUID del dueño desde el nombre del mundo
                        UUID ownerId = UUID.fromString(world.getName().replace("island_", ""));
                        islandManager.unloadIslandSafe(ownerId);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("No se pudo extraer UUID del mundo: " + world.getName());
                    }
                }
            }, 2L);
        }
    }
}
package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

                    // 🌟 FIX: Usamos el método nativo que busca la isla por ubicación
                    // Pasamos el centro exacto del mundo para que nos devuelva el perfil
                    IslandProfile profile = islandManager.getIslandAt(new Location(world, 0, 0, 0));

                    if (profile != null) {
                        islandManager.unloadIslandSafe(profile);
                    } else {
                        plugin.getLogger().warning("Intento de apagar isla sin perfil en RAM: " + world.getName());
                        // Por seguridad, si el perfil no existe pero el mundo está vacío, forzamos apagado
                        Bukkit.unloadWorld(world, true);
                    }
                }
            }, 2L);
        }
    }
}
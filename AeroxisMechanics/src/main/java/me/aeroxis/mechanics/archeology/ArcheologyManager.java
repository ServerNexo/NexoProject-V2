package me.aeroxis.mechanics.archeology;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.mechanics.AeroxisMechanics;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Singleton
public class ArcheologyManager {

    private final AeroxisMechanics plugin;
    private final Logger logger;

    // 🌟 Caché de tesoros INVISIBLES (Coordenadas secretas)
    private final Set<Location> hiddenTreasures = ConcurrentHashMap.newKeySet();

    @Inject
    public ArcheologyManager(AeroxisMechanics plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public Set<Location> getHiddenTreasures() {
        return hiddenTreasures;
    }

    public void removeTreasure(Location loc) {
        hiddenTreasures.remove(loc);
    }

    /**
     * 🗺️ Selecciona coordenadas aleatorias pero NO modifica el mapa (Estilo Hypixel)
     */
    public void generateHiddenTreasures(Location center, int radius, int amount) {
        if (center == null || center.getWorld() == null) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            int spawned = 0;

            for (int i = 0; i < amount * 10; i++) {
                if (spawned >= amount) break;

                double angle = Math.random() * Math.PI * 2;
                double r = Math.sqrt(Math.random()) * radius;
                int x = (int) (center.getX() + r * Math.cos(angle));
                int z = (int) (center.getZ() + r * Math.sin(angle));

                Block highest = center.getWorld().getHighestBlockAt(x, z);

                // Bajamos hasta tocar suelo sólido
                while (highest.getY() > center.getWorld().getMinHeight()) {
                    if (highest.getType().isSolid() &&
                            !highest.getType().name().contains("LEAVES") &&
                            !highest.getType().name().contains("LOG")) {
                        break;
                    }
                    highest = highest.getRelative(BlockFace.DOWN);
                }

                // Si encontramos un bloque sólido (arena, pasto, tierra, etc.)
                if (highest.getType().isSolid()) {
                    Location loc = highest.getLocation();

                    if (!hiddenTreasures.contains(loc)) {
                        // 🌟 MAGIA HYPIXEL: Solo guardamos la coordenada en RAM. El bloque sigue siendo normal.
                        hiddenTreasures.add(loc);
                        spawned++;
                    }
                }
            }
            logger.info("🏺 Evento Hypixel: Se han ocultado " + spawned + " tesoros invisibles en el mapa.");
        });
    }

    public void cleanupAllSpots() {
        hiddenTreasures.clear();
        logger.info("🧹 Arqueología: Caché de tesoros invisibles limpiado.");
    }
}
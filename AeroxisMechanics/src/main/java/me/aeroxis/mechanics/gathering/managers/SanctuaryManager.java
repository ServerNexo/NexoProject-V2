package me.aeroxis.mechanics.gathering.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.mechanics.AeroxisMechanics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 🕊️ SanctuaryManager - Motor de Áreas de Paz (Inciensos)
 * Rendimiento Extremo mediante Partición Espacial (Chunk Hashing).
 */
@Singleton
public class SanctuaryManager {

    private final AeroxisMechanics plugin;

    // Map<ChunkKey, Map<Location, ExpirationTime>>
    private final Map<Long, Map<Location, Long>> activeIncenses = new ConcurrentHashMap<>();
    private static final double INCENSE_RADIUS_SQR = 15.0 * 15.0; // Distancia al cuadrado (225.0) para matemáticas rápidas

    @Inject
    public SanctuaryManager(AeroxisMechanics plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    public void activateIncense(Location loc, long durationMillis) {
        long chunkKey = getChunkKey(loc);
        long expiration = System.currentTimeMillis() + durationMillis;

        activeIncenses.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>()).put(loc, expiration);
    }

    public boolean isProtected(Player player) {
        Location pLoc = player.getLocation();
        int chunkX = pLoc.getBlockX() >> 4;
        int chunkZ = pLoc.getBlockZ() >> 4;
        long now = System.currentTimeMillis();

        // Escaneamos el Chunk actual del jugador y los 8 Chunks vecinos (Cuadrícula 3x3)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = getChunkKey(chunkX + dx, chunkZ + dz);
                Map<Location, Long> chunkIncenses = activeIncenses.get(key);

                if (chunkIncenses != null) {
                    for (Map.Entry<Location, Long> entry : chunkIncenses.entrySet()) {
                        if (now <= entry.getValue() && entry.getKey().getWorld().equals(pLoc.getWorld())) {
                            if (entry.getKey().distanceSquared(pLoc) <= INCENSE_RADIUS_SQR) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // ==========================================
    // 🧹 GESTIÓN DE MEMORIA (Folia Async Task)
    // ==========================================
    private void startCleanupTask() {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            long now = System.currentTimeMillis();

            for (Map<Location, Long> chunkMap : activeIncenses.values()) {
                chunkMap.entrySet().removeIf(entry -> now > entry.getValue());
            }
            activeIncenses.values().removeIf(Map::isEmpty);

        }, 10, 10, TimeUnit.SECONDS);
    }

    private long getChunkKey(Location loc) {
        return getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private long getChunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }
}
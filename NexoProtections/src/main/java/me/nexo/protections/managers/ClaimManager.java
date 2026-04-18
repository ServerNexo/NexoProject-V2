package me.nexo.protections.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ClaimBox;
import me.nexo.protections.core.ProtectionStone;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * 🛡️ NexoProtections - Gestor Espacial de Claims (Arquitectura Enterprise Java 25)
 * Rendimiento: Spatial Grid "Zero-Garbage" O(1), Llaves Bitwise (Long) y Hilos Virtuales.
 */
@Singleton
public class ClaimManager {

    private final NexoProtections plugin;
    private final DatabaseManager databaseManager;

    // 🌟 OPTIMIZACIÓN O(1) ZERO-GARBAGE: Mapa anidado World -> ChunkKey(Long) -> Lista de Claims
    private final Map<String, Map<Long, List<ProtectionStone>>> spatialGrid = new ConcurrentHashMap<>();
    private final Map<UUID, ProtectionStone> stonesById = new ConcurrentHashMap<>();

    @Inject
    public ClaimManager(NexoProtections plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager; // 💉 Desacoplado de NexoCore
    }

    // 🌟 MAGIA ENTERPRISE: Utilidad matemática pura para llaves de chunks sin crear Strings.
    // Combinamos la X y la Z en un solo número "Long" usando Bit-Shifting. Cero impacto a la RAM.
    private long getChunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    // =========================================================================
    // 🗺️ MOTOR ESPACIAL
    // =========================================================================
    public boolean hasOverlappingClaim(ClaimBox newBox) {
        int minChunkX = newBox.minX() >> 4;
        int maxChunkX = newBox.maxX() >> 4;
        int minChunkZ = newBox.minZ() >> 4;
        int maxChunkZ = newBox.maxZ() >> 4;

        Map<Long, List<ProtectionStone>> worldGrid = spatialGrid.get(newBox.world());
        if (worldGrid == null) return false;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                List<ProtectionStone> stonesInChunk = worldGrid.get(getChunkKey(cx, cz));
                if (stonesInChunk != null) {
                    for (ProtectionStone stone : stonesInChunk) {
                        if (stone.getBox().intersects(newBox)) return true;
                    }
                }
            }
        }
        return false;
    }

    public void addStoneToCache(ProtectionStone stone) {
        stonesById.put(stone.getStoneId(), stone);
        int minChunkX = stone.getBox().minX() >> 4;
        int maxChunkX = stone.getBox().maxX() >> 4;
        int minChunkZ = stone.getBox().minZ() >> 4;
        int maxChunkZ = stone.getBox().maxZ() >> 4;

        Map<Long, List<ProtectionStone>> worldGrid = spatialGrid.computeIfAbsent(stone.getBox().world(), k -> new ConcurrentHashMap<>());

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                // 🌟 FIX C.M.E: CopyOnWriteArrayList garantiza que si un jugador corre por la zona
                // mientras otro pone un bloque, el servidor no colapse por concurrencia.
                worldGrid.computeIfAbsent(getChunkKey(cx, cz), k -> new CopyOnWriteArrayList<>()).add(stone);
            }
        }
    }

    public void removeStoneFromCache(ProtectionStone stone) {
        stonesById.remove(stone.getStoneId());
        int minChunkX = stone.getBox().minX() >> 4;
        int maxChunkX = stone.getBox().maxX() >> 4;
        int minChunkZ = stone.getBox().minZ() >> 4;
        int maxChunkZ = stone.getBox().maxZ() >> 4;

        Map<Long, List<ProtectionStone>> worldGrid = spatialGrid.get(stone.getBox().world());
        if (worldGrid == null) return;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long key = getChunkKey(cx, cz);
                List<ProtectionStone> stonesInChunk = worldGrid.get(key);
                if (stonesInChunk != null) {
                    stonesInChunk.remove(stone);
                    // 🌟 FIX TOCTOU: Eliminado el check de isEmpty() y el worldGrid.remove().
                    // La lista vacía permanecerá en RAM asegurando transacciones futuras seguras.
                }
            }
        }
    }

    public ProtectionStone getStoneAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        Map<Long, List<ProtectionStone>> worldGrid = spatialGrid.get(loc.getWorld().getName());
        if (worldGrid == null) return null;

        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        // 🚀 Búsqueda O(1) Instantánea sin Strings
        List<ProtectionStone> stonesInChunk = worldGrid.get(getChunkKey(cx, cz));

        if (stonesInChunk != null) {
            for (ProtectionStone stone : stonesInChunk) {
                if (stone.getBox().contains(loc)) return stone;
            }
        }
        return null;
    }

    public ProtectionStone getStoneById(UUID id) { return stonesById.get(id); }
    public Map<UUID, ProtectionStone> getAllStones() { return stonesById; }

    // =========================================================================
    // 🌟 GUARDADO ASÍNCRONO MASIVO EN HILOS VIRTUALES
    // =========================================================================
    public void saveStoneDataAsync(ProtectionStone stone) {
        // 🚀 Reemplazado CompletableFuture por Hilos Virtuales nativos
        Thread.startVirtualThread(() -> {
            StringBuilder membersStr = new StringBuilder();
            for (UUID uuid : stone.getTrustedFriends()) {
                membersStr.append(uuid.toString()).append(",");
            }

            StringBuilder flagsStr = new StringBuilder();
            for (Map.Entry<String, Boolean> entry : stone.getFlags().entrySet()) {
                flagsStr.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
            }

            String sql = "UPDATE nexo_protections SET members = ?, flags = ? WHERE stone_id = CAST(? AS UUID)";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {

                ps.setString(1, membersStr.toString());
                ps.setString(2, flagsStr.toString());
                ps.setString(3, stone.getStoneId().toString());
                ps.executeUpdate();

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error guardando datos de la piedra " + stone.getStoneId(), e);
            }
        });
    }

    // =========================================================================
    // 🌟 CARGA INICIAL DESDE LA BASE DE DATOS
    // =========================================================================
    public void loadAllStonesAsync() {
        Thread.startVirtualThread(() -> {
            String sql = "SELECT * FROM nexo_protections";
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql);
                 var rs = ps.executeQuery()) {

                int loaded = 0;
                while(rs.next()) {
                    UUID stoneId = UUID.fromString(rs.getString("stone_id"));
                    UUID ownerId = UUID.fromString(rs.getString("owner_id"));
                    String clanStr = rs.getString("clan_id");
                    UUID clanId = (clanStr != null && !clanStr.isEmpty()) ? UUID.fromString(clanStr) : null;

                    String world = rs.getString("world_name");
                    int minX = rs.getInt("min_x");
                    int minY = rs.getInt("min_y");
                    int minZ = rs.getInt("min_z");
                    int maxX = rs.getInt("max_x");
                    int maxY = rs.getInt("max_y");
                    int maxZ = rs.getInt("max_z");

                    ClaimBox box = new ClaimBox(world, minX, minY, minZ, maxX, maxY, maxZ);
                    ProtectionStone stone = new ProtectionStone(stoneId, ownerId, clanId, box);

                    stone.drainEnergy(100000); // Reseteo simulado
                    stone.setMaxEnergy(rs.getDouble("max_energy"));
                    stone.addEnergy(rs.getDouble("current_energy"));

                    String membersStr = rs.getString("members");
                    if (membersStr != null && !membersStr.isEmpty()) {
                        for (String uuidStr : membersStr.split(",")) {
                            if (!uuidStr.isEmpty()) stone.addFriend(UUID.fromString(uuidStr));
                        }
                    }

                    String flagsStr = rs.getString("flags");
                    if (flagsStr != null && !flagsStr.isEmpty()) {
                        for (String flagData : flagsStr.split(",")) {
                            if (!flagData.isEmpty() && flagData.contains(":")) {
                                String[] parts = flagData.split(":");
                                stone.setFlag(parts[0], Boolean.parseBoolean(parts[1]));
                            }
                        }
                    }

                    addStoneToCache(stone);
                    loaded++;
                }
                plugin.getLogger().info("🛡️ NexoProtections: Se cargaron " + loaded + " zonas protegidas (Con Acólitos y Leyes).");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error cargando piedras de protección", e);
            }
        });
    }
}
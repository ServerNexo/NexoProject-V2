package me.nexo.islas.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;
import me.nexo.islas.NexoIslas;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class IslandDatabase {

    private final NexoIslas plugin;
    private final DatabaseManager db;

    @Inject
    public IslandDatabase(NexoIslas plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        createTable();
    }

    private void createTable() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String sql = """
                CREATE TABLE IF NOT EXISTS nexo_islands (
                    owner_id UUID PRIMARY KEY,
                    grid_index SERIAL, 
                    members TEXT,
                    border_level INT DEFAULT 1,
                    member_limit INT DEFAULT 4,
                    is_locked BOOLEAN DEFAULT FALSE,
                    wealth_score DOUBLE PRECISION DEFAULT 0.0,
                    activity_score DOUBLE PRECISION DEFAULT 0.0
                );
            """;
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error creando tabla de islas: " + e.getMessage());
            }
        });
    }

    /**
     * 📥 Carga el perfil de la isla. Retorna null si el jugador aún no tiene una.
     */
    public CompletableFuture<IslandProfile> loadIsland(UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM nexo_islands WHERE owner_id = CAST(? AS UUID)";

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, ownerId.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    IslandProfile profile = new IslandProfile(ownerId, rs.getInt("grid_index"));
                    profile.setBorderLevel(rs.getInt("border_level"));
                    profile.setMemberLimit(rs.getInt("member_limit"));
                    profile.setLocked(rs.getBoolean("is_locked"));
                    profile.setWealthScore(rs.getDouble("wealth_score"));
                    profile.setActivityScore(rs.getDouble("activity_score"));

                    String membersRaw = rs.getString("members");
                    if (membersRaw != null && !membersRaw.isEmpty()) {
                        List<UUID> memberUuids = Arrays.stream(membersRaw.split(","))
                                .map(UUID::fromString)
                                .collect(Collectors.toList());
                        profile.setMembers(memberUuids);
                    }
                    return profile;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando perfil de isla: " + e.getMessage());
            }
            return null; // Retorna null para que el Manager sepa que debe crear una nueva
        });
    }

    /**
     * 🌟 NUEVO: Crea el registro inicial y nos devuelve el ID autoincremental del Grid
     */
    public CompletableFuture<Integer> createNewIsland(UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Insertamos la isla y pedimos que nos devuelva el grid_index generado
            String sql = "INSERT INTO nexo_islands (owner_id, members) VALUES (CAST(? AS UUID), '') RETURNING grid_index";

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, ownerId.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return rs.getInt(1); // Devolvemos la posición en la cuadrícula
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error creando nueva isla en BD: " + e.getMessage());
            }
            return -1;
        });
    }

    /**
     * 💾 Guarda o actualiza los datos en tiempo real
     */
    public void saveIslandSync(IslandProfile profile) {
        String sql = """
            INSERT INTO nexo_islands (owner_id, grid_index, members, border_level, member_limit, is_locked, wealth_score, activity_score)
            VALUES (CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (owner_id) DO UPDATE SET
            members = EXCLUDED.members,
            border_level = EXCLUDED.border_level,
            member_limit = EXCLUDED.member_limit,
            is_locked = EXCLUDED.is_locked,
            wealth_score = EXCLUDED.wealth_score,
            activity_score = EXCLUDED.activity_score;
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String membersStr = profile.getMembers().stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            ps.setString(1, profile.getOwnerId().toString());
            ps.setInt(2, profile.getGridIndex());
            ps.setString(3, membersStr);
            ps.setInt(4, profile.getBorderLevel());
            ps.setInt(5, profile.getMemberLimit());
            ps.setBoolean(6, profile.isLocked());
            ps.setDouble(7, profile.getWealthScore());
            ps.setDouble(8, profile.getActivityScore());

            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error guardando perfil de isla: " + e.getMessage());
        }
    }
}
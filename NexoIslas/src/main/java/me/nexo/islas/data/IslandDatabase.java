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
            // 🌟 ESQUEMA AAA: Incluye todos los Tiers de Mejoras y Puntos
            String sql = """
                CREATE TABLE IF NOT EXISTS nexo_islands (
                    owner_id UUID PRIMARY KEY,
                    grid_index SERIAL, 
                    members TEXT,
                    is_locked BOOLEAN DEFAULT FALSE,
                    wealth_score DOUBLE PRECISION DEFAULT 0.0,
                    activity_score DOUBLE PRECISION DEFAULT 0.0,
                    
                    upgrade_points INT DEFAULT 0,
                    border_level INT DEFAULT 1,
                    member_limit_level INT DEFAULT 1,
                    minion_limit_level INT DEFAULT 1,
                    spawner_limit_level INT DEFAULT 1,
                    factory_limit_level INT DEFAULT 1,
                    crop_growth_level INT DEFAULT 1,
                    spawner_rate_level INT DEFAULT 1,
                    mob_drop_level INT DEFAULT 1,
                    farming_drop_level INT DEFAULT 1,
                    generator_level INT DEFAULT 1,
                    xp_bonus_level INT DEFAULT 1
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

                    // Configuración General
                    profile.setLocked(rs.getBoolean("is_locked"));
                    profile.setWealthScore(rs.getDouble("wealth_score"));
                    profile.setActivityScore(rs.getDouble("activity_score"));

                    // 🌟 CARGAMOS LA ECONOMÍA DE MEJORAS
                    profile.setUpgradePoints(rs.getInt("upgrade_points"));
                    profile.setBorderLevel(rs.getInt("border_level"));
                    profile.setMemberLimitLevel(rs.getInt("member_limit_level"));
                    profile.setMinionLimitLevel(rs.getInt("minion_limit_level"));
                    profile.setSpawnerLimitLevel(rs.getInt("spawner_limit_level"));
                    profile.setFactoryLimitLevel(rs.getInt("factory_limit_level"));
                    profile.setCropGrowthLevel(rs.getInt("crop_growth_level"));
                    profile.setSpawnerRateLevel(rs.getInt("spawner_rate_level"));
                    profile.setMobDropLevel(rs.getInt("mob_drop_level"));
                    profile.setFarmingDropLevel(rs.getInt("farming_drop_level"));
                    profile.setGeneratorLevel(rs.getInt("generator_level"));
                    profile.setXpBonusLevel(rs.getInt("xp_bonus_level"));

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
     * 💾 Guarda o actualiza los datos en tiempo real (Upsert)
     */
    public void saveIslandSync(IslandProfile profile) {
        String sql = """
            INSERT INTO nexo_islands (owner_id, grid_index, members, is_locked, wealth_score, activity_score, 
                upgrade_points, border_level, member_limit_level, minion_limit_level, spawner_limit_level, 
                factory_limit_level, crop_growth_level, spawner_rate_level, mob_drop_level, farming_drop_level, 
                generator_level, xp_bonus_level)
            VALUES (CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (owner_id) DO UPDATE SET
                members = EXCLUDED.members,
                is_locked = EXCLUDED.is_locked,
                wealth_score = EXCLUDED.wealth_score,
                activity_score = EXCLUDED.activity_score,
                upgrade_points = EXCLUDED.upgrade_points,
                border_level = EXCLUDED.border_level,
                member_limit_level = EXCLUDED.member_limit_level,
                minion_limit_level = EXCLUDED.minion_limit_level,
                spawner_limit_level = EXCLUDED.spawner_limit_level,
                factory_limit_level = EXCLUDED.factory_limit_level,
                crop_growth_level = EXCLUDED.crop_growth_level,
                spawner_rate_level = EXCLUDED.spawner_rate_level,
                mob_drop_level = EXCLUDED.mob_drop_level,
                farming_drop_level = EXCLUDED.farming_drop_level,
                generator_level = EXCLUDED.generator_level,
                xp_bonus_level = EXCLUDED.xp_bonus_level;
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String membersStr = profile.getMembers().stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            // Set variables
            ps.setString(1, profile.getOwnerId().toString());
            ps.setInt(2, profile.getGridIndex());
            ps.setString(3, membersStr);
            ps.setBoolean(4, profile.isLocked());
            ps.setDouble(5, profile.getWealthScore());
            ps.setDouble(6, profile.getActivityScore());

            // Set Mejoras
            ps.setInt(7, profile.getUpgradePoints());
            ps.setInt(8, profile.getBorderLevel());
            ps.setInt(9, profile.getMemberLimitLevel());
            ps.setInt(10, profile.getMinionLimitLevel());
            ps.setInt(11, profile.getSpawnerLimitLevel());
            ps.setInt(12, profile.getFactoryLimitLevel());
            ps.setInt(13, profile.getCropGrowthLevel());
            ps.setInt(14, profile.getSpawnerRateLevel());
            ps.setInt(15, profile.getMobDropLevel());
            ps.setInt(16, profile.getFarmingDropLevel());
            ps.setInt(17, profile.getGeneratorLevel());
            ps.setInt(18, profile.getXpBonusLevel());

            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error guardando perfil de isla: " + e.getMessage());
        }
    }
}
package me.nexo.islas.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;
import me.nexo.islas.NexoIslas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class IslandDatabase {

    private final NexoIslas plugin;
    private final DatabaseManager db;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public IslandDatabase(NexoIslas plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        createTable();
    }

    private void createTable() {
        virtualExecutor.submit(() -> {
            // 🌟 TABLA DE ISLAS (Se eliminó island_xp)
            String sql = """
                CREATE TABLE IF NOT EXISTS nexo_islands (
                    island_id UUID UNIQUE, 
                    owner_id UUID PRIMARY KEY,
                    island_name VARCHAR(64),
                    grid_index SERIAL, 
                    is_locked BOOLEAN DEFAULT FALSE,
                    
                    island_level INT DEFAULT 1,
                    island_value INT DEFAULT 0,
                    
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

            // 🌟 TABLA DE MIEMBROS (Añadido contributed_xp para guardar el progreso individual)
            String sqlMembers = """
                CREATE TABLE IF NOT EXISTS nexo_island_members (
                    island_id UUID REFERENCES nexo_islands(island_id) ON DELETE CASCADE,
                    player_id UUID,
                    role_id VARCHAR(16) DEFAULT 'MEMBER',
                    contributed_xp DOUBLE PRECISION DEFAULT 0.0,
                    PRIMARY KEY (island_id, player_id)
                );
            """;

            try (Connection conn = db.getConnection();
                 PreparedStatement stmt1 = conn.prepareStatement(sql);
                 PreparedStatement stmt2 = conn.prepareStatement(sqlMembers)) {
                stmt1.execute();
                stmt2.execute();
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error creando tabla de islas: " + e.getMessage());
            }
        });
    }

    /**
     * 📥 Carga el perfil de la isla y sus miembros. Retorna null si no tiene.
     */
    public CompletableFuture<IslandProfile> loadIsland(UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sqlIsland = "SELECT * FROM nexo_islands WHERE owner_id = CAST(? AS UUID)";
            String sqlMembers = "SELECT player_id, role_id, contributed_xp FROM nexo_island_members WHERE island_id = CAST(? AS UUID)";

            try (Connection conn = db.getConnection();
                 PreparedStatement psIsland = conn.prepareStatement(sqlIsland)) {

                psIsland.setString(1, ownerId.toString());
                ResultSet rs = psIsland.executeQuery();

                if (rs.next()) {
                    UUID islandId = UUID.fromString(rs.getString("island_id"));
                    IslandProfile profile = new IslandProfile(
                            islandId,
                            ownerId,
                            rs.getInt("grid_index"),
                            rs.getString("island_name")
                    );

                    profile.setLocked(rs.getBoolean("is_locked"));

                    // 🌟 TOP SISTEMA
                    profile.setLevel(rs.getInt("island_level"));
                    profile.setValue(rs.getInt("island_value"));

                    // 🌟 ECONOMÍA DE MEJORAS
                    profile.addUpgradePoints(rs.getInt("upgrade_points"));
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

                    // 👥 CARGAR MIEMBROS, ROLES Y SU XP APORTADA
                    try (PreparedStatement psMembers = conn.prepareStatement(sqlMembers)) {
                        psMembers.setString(1, islandId.toString());
                        ResultSet rsMembers = psMembers.executeQuery();
                        while (rsMembers.next()) {
                            UUID memberId = UUID.fromString(rsMembers.getString("player_id"));
                            IslandRole role = IslandRole.valueOf(rsMembers.getString("role_id"));
                            double xpAportada = rsMembers.getDouble("contributed_xp");

                            // Si es miembro co-op normal lo añadimos a la lista
                            if (!memberId.equals(ownerId)) {
                                profile.addMember(memberId, role);
                            }

                            // Le inyectamos su XP al mapa (Incluso al dueño)
                            profile.addPlayerXp(memberId, xpAportada);
                        }
                    }
                    return profile;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando perfil de isla: " + e.getMessage());
            }
            return null;
        }, virtualExecutor);
    }

    /**
     * 🌟 NUEVO: Crea el registro inicial. Devuelve el Profile básico sin cargar en RAM aún.
     */
    public CompletableFuture<IslandProfile> createNewIsland(UUID ownerId, String islandName) {
        return CompletableFuture.supplyAsync(() -> {
            UUID islandId = UUID.randomUUID();
            String sql = "INSERT INTO nexo_islands (island_id, owner_id, island_name) VALUES (CAST(? AS UUID), CAST(? AS UUID), ?) RETURNING grid_index";

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, islandId.toString());
                ps.setString(2, ownerId.toString());
                ps.setString(3, islandName);

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    IslandProfile profile = new IslandProfile(islandId, ownerId, rs.getInt(1), islandName);
                    // Insertamos al dueño en la tabla de miembros (Para que pueda tener XP)
                    saveMembersAsync(profile);
                    return profile;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error creando nueva isla en BD: " + e.getMessage());
            }
            return null;
        }, virtualExecutor);
    }

    /**
     * 💾 Guarda o actualiza los datos en tiempo real (Upsert)
     */
    public void saveIslandSync(IslandProfile profile) {
        String sql = """
            INSERT INTO nexo_islands (island_id, owner_id, island_name, grid_index, is_locked, 
                island_level, island_value, upgrade_points, border_level, member_limit_level, 
                minion_limit_level, spawner_limit_level, factory_limit_level, crop_growth_level, 
                spawner_rate_level, mob_drop_level, farming_drop_level, generator_level, xp_bonus_level)
            VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (owner_id) DO UPDATE SET
                island_name = EXCLUDED.island_name,
                is_locked = EXCLUDED.is_locked,
                island_level = EXCLUDED.island_level,
                island_value = EXCLUDED.island_value,
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

            ps.setString(1, profile.getIslandId().toString());
            ps.setString(2, profile.getOwnerId().toString());
            ps.setString(3, profile.getIslandName());
            ps.setInt(4, profile.getGridIndex());
            ps.setBoolean(5, profile.isLocked());

            ps.setInt(6, profile.getLevel());
            ps.setInt(7, profile.getValue());

            ps.setInt(8, profile.getUpgradePoints());
            ps.setInt(9, profile.getBorderLevel());
            ps.setInt(10, profile.getMemberLimitLevel());
            ps.setInt(11, profile.getMinionLimitLevel());
            ps.setInt(12, profile.getSpawnerLimitLevel());
            ps.setInt(13, profile.getFactoryLimitLevel());
            ps.setInt(14, profile.getCropGrowthLevel());
            ps.setInt(15, profile.getSpawnerRateLevel());
            ps.setInt(16, profile.getMobDropLevel());
            ps.setInt(17, profile.getFarmingDropLevel());
            ps.setInt(18, profile.getGeneratorLevel());
            ps.setInt(19, profile.getXpBonusLevel());

            ps.executeUpdate();

            // 👥 Guardado Asíncrono de Miembros y XP
            saveMembersAsync(profile);

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error guardando perfil de isla: " + e.getMessage());
        }
    }

    private void saveMembersAsync(IslandProfile profile) {
        virtualExecutor.submit(() -> {
            String deleteSql = "DELETE FROM nexo_island_members WHERE island_id = CAST(? AS UUID)";
            String insertSql = "INSERT INTO nexo_island_members (island_id, player_id, role_id, contributed_xp) VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?)";

            try (Connection conn = db.getConnection();
                 PreparedStatement psDel = conn.prepareStatement(deleteSql);
                 PreparedStatement psIns = conn.prepareStatement(insertSql)) {

                // Borramos a todos los miembros actuales (Limpieza)
                psDel.setString(1, profile.getIslandId().toString());
                psDel.executeUpdate();

                // 1. Guardamos al Dueño primero (Su rol es OWNER, leemos su XP del mapa)
                psIns.setString(1, profile.getIslandId().toString());
                psIns.setString(2, profile.getOwnerId().toString());
                psIns.setString(3, IslandRole.OWNER.name());
                psIns.setDouble(4, profile.getMemberXpContributions().getOrDefault(profile.getOwnerId(), 0.0));
                psIns.addBatch();

                // 2. Guardamos al resto de miembros y su XP
                for (UUID memberId : profile.getMembers().keySet()) {
                    psIns.setString(1, profile.getIslandId().toString());
                    psIns.setString(2, memberId.toString());
                    psIns.setString(3, profile.getRole(memberId).name());
                    psIns.setDouble(4, profile.getMemberXpContributions().getOrDefault(memberId, 0.0));
                    psIns.addBatch();
                }

                psIns.executeBatch();
            } catch (SQLException e) {
                plugin.getLogger().severe("❌ Error guardando miembros de la isla: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // 🏆 CONSULTAS LEADERBOARD (TOP 10)
    // ==========================================

    public record IslandTopEntry(String islandName, String ownerName, int score) {}

    public CompletableFuture<List<IslandTopEntry>> getTopIslandsByLevel() {
        return CompletableFuture.supplyAsync(() -> {
            List<IslandTopEntry> top = new ArrayList<>();
            // Ahora ordenamos puramente por Nivel. Si queremos desempate, podríamos sumar la XP de nexo_island_members,
            // pero por rendimiento lo dejamos por Nivel y luego por Valor Riqueza.
            String sql = "SELECT island_name, owner_id, island_level FROM nexo_islands ORDER BY island_level DESC, island_value DESC LIMIT 10";

            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.add(new IslandTopEntry(rs.getString("island_name"), rs.getString("owner_id"), rs.getInt("island_level")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return top;
        }, virtualExecutor);
    }

    public CompletableFuture<List<IslandTopEntry>> getTopIslandsByValue() {
        return CompletableFuture.supplyAsync(() -> {
            List<IslandTopEntry> top = new ArrayList<>();
            String sql = "SELECT island_name, owner_id, island_value FROM nexo_islands ORDER BY island_value DESC LIMIT 10";

            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.add(new IslandTopEntry(rs.getString("island_name"), rs.getString("owner_id"), rs.getInt("island_value")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return top;
        }, virtualExecutor);
    }
}
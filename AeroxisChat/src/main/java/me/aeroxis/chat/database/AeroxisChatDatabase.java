package me.aeroxis.chat.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.chat.managers.AeroxisChatManager;
import me.aeroxis.core.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
public class AeroxisChatDatabase {

    private final DatabaseManager databaseManager;
    private final AeroxisChatManager chatManager;

    @Inject
    public AeroxisChatDatabase(DatabaseManager databaseManager, AeroxisChatManager chatManager) {
        this.databaseManager = databaseManager;
        this.chatManager = chatManager;
    }

    public void createTables() {
        try (Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            // 1. Tabla de perfiles (Solo para datos específicos de Chat)
            stmt.execute("CREATE TABLE IF NOT EXISTS nexochat_profiles (" +
                    "uuid VARCHAR(36) PRIMARY KEY);");

            // 2. Tabla de ignorados
            stmt.execute("CREATE TABLE IF NOT EXISTS nexochat_ignores (" +
                    "uuid VARCHAR(36), " +
                    "ignored_uuid VARCHAR(36), " +
                    "PRIMARY KEY (uuid, ignored_uuid));");

            // 🌟 3. MIGRACIONES SEGURAS (Nicks, Tags y Muteos)
            try { stmt.execute("ALTER TABLE nexochat_profiles ADD COLUMN IF NOT EXISTS mute_end BIGINT DEFAULT 0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE nexochat_profiles ADD COLUMN IF NOT EXISTS mute_reason VARCHAR(255) DEFAULT '';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE nexochat_profiles ADD COLUMN IF NOT EXISTS nickname VARCHAR(32) DEFAULT '';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE nexochat_profiles ADD COLUMN IF NOT EXISTS active_tag VARCHAR(64) DEFAULT '';"); } catch (Exception ignored) {}

            // 4. Tags desbloqueados (Específicos de este módulo)
            stmt.execute("CREATE TABLE IF NOT EXISTS nexochat_unlocked_tags (" +
                    "uuid VARCHAR(36), " +
                    "tag_id VARCHAR(64), " +
                    "PRIMARY KEY (uuid, tag_id));");

            Bukkit.getLogger().info("✅ [NexoChat] Base de Datos sincronizada con la arquitectura Core.");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ [NexoChat] Error creando tablas: " + e.getMessage());
        }
    }

    public void loadPlayerData(UUID uuid) {
        try (Connection conn = databaseManager.getConnection()) {

            // 🌟 Cargar Nicks, Tags y Muteos
            String queryProfile = "SELECT nickname, active_tag, mute_end, mute_reason FROM nexochat_profiles WHERE uuid = ?;";
            try (PreparedStatement ps = conn.prepareStatement(queryProfile)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    chatManager.setPlayerNickname(uuid, rs.getString("nickname"));
                    chatManager.setPlayerActiveTag(uuid, rs.getString("active_tag"));

                    long muteEnd = rs.getLong("mute_end");
                    if (muteEnd > System.currentTimeMillis()) {
                        chatManager.setMute(uuid, muteEnd, rs.getString("mute_reason"));
                    }
                }
            }

            // Cargar Ignorados
            String queryIgnores = "SELECT ignored_uuid FROM nexochat_ignores WHERE uuid = ?;";
            Set<UUID> ignoredSet = new HashSet<>();
            try (PreparedStatement ps = conn.prepareStatement(queryIgnores)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ignoredSet.add(UUID.fromString(rs.getString("ignored_uuid")));
                }
            }
            chatManager.getIgnoredPlayersMap().put(uuid, ignoredSet);

            // Cargar Tags Desbloqueados
            String queryTags = "SELECT tag_id FROM nexochat_unlocked_tags WHERE uuid = ?;";
            Set<String> unlockedTags = new HashSet<>();
            try (PreparedStatement ps = conn.prepareStatement(queryTags)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    unlockedTags.add(rs.getString("tag_id"));
                }
            }
            chatManager.getUnlockedTagsMap().put(uuid, unlockedTags);

        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ [NexoChat] Error cargando datos de " + uuid + ": " + e.getMessage());
        }
    }

    public void savePlayerData(UUID uuid, Set<UUID> ignoredPlayers) {
        try (Connection conn = databaseManager.getConnection()) {

            String nickname = chatManager.getPlayerNickname(uuid);
            String activeTag = chatManager.getPlayerActiveTag(uuid);
            long muteEnd = chatManager.getMuteEndTimes().getOrDefault(uuid, 0L);
            String muteReason = chatManager.getMuteReasons().getOrDefault(uuid, "");

            // 🌟 Upsert de datos de Chat
            String upsertProfile = "INSERT INTO nexochat_profiles (uuid, nickname, active_tag, mute_end, mute_reason) VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (uuid) DO UPDATE SET nickname = EXCLUDED.nickname, active_tag = EXCLUDED.active_tag, " +
                    "mute_end = EXCLUDED.mute_end, mute_reason = EXCLUDED.mute_reason;";

            try (PreparedStatement ps = conn.prepareStatement(upsertProfile)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, nickname != null ? nickname : "");
                ps.setString(3, activeTag != null ? activeTag : "");
                ps.setLong(4, muteEnd);
                ps.setString(5, muteReason);
                ps.executeUpdate();
            }

            // Guardar Ignorados
            stmtExecuteBatch(conn, "DELETE FROM nexochat_ignores WHERE uuid = ?;", uuid);
            if (ignoredPlayers != null && !ignoredPlayers.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO nexochat_ignores (uuid, ignored_uuid) VALUES (?, ?);")) {
                    for (UUID ignored : ignoredPlayers) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, ignored.toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // Guardar Tags
            stmtExecuteBatch(conn, "DELETE FROM nexochat_unlocked_tags WHERE uuid = ?;", uuid);
            Set<String> unlockedTags = chatManager.getUnlockedTagsMap().get(uuid);
            if (unlockedTags != null && !unlockedTags.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO nexochat_unlocked_tags (uuid, tag_id) VALUES (?, ?);")) {
                    for (String tag : unlockedTags) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, tag);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ [NexoChat] Error guardando datos de " + uuid + ": " + e.getMessage());
        }
    }

    private void stmtExecuteBatch(Connection conn, String sql, UUID uuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void muteOfflinePlayer(UUID target, long muteEnd, String reason) {
        Bukkit.getAsyncScheduler().runNow(Bukkit.getPluginManager().getPlugin("NexoChat"), task -> {
            try (Connection conn = databaseManager.getConnection()) {
                String upsert = "INSERT INTO nexochat_profiles (uuid, mute_end, mute_reason) VALUES (?, 0, '', ?, ?) " +
                        "ON CONFLICT (uuid) DO UPDATE SET mute_end = EXCLUDED.mute_end, mute_reason = EXCLUDED.mute_reason;";
                try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                    ps.setString(1, target.toString());
                    ps.setLong(2, muteEnd);
                    ps.setString(3, reason);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Error muteando offline: " + e.getMessage());
            }
        });
    }
}
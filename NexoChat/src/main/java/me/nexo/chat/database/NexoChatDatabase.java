package me.nexo.chat.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.chat.managers.NexoChatManager;
import me.nexo.core.database.DatabaseManager; // 🌟 Tu Manager del Core
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
public class NexoChatDatabase {

    private final DatabaseManager databaseManager;
    private final NexoChatManager chatManager;

    @Inject
    public NexoChatDatabase(DatabaseManager databaseManager, NexoChatManager chatManager) {
        this.databaseManager = databaseManager;
        this.chatManager = chatManager;
    }

    public void createTables() {
        try (Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            // 1. Crear tabla base
            stmt.execute("CREATE TABLE IF NOT EXISTS nexochat_profiles (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "cosmetic VARCHAR(64) NOT NULL DEFAULT '<gray>');");

            // 2. Crear tabla de ignorados
            stmt.execute("CREATE TABLE IF NOT EXISTS nexochat_ignores (" +
                    "uuid VARCHAR(36), " +
                    "ignored_uuid VARCHAR(36), " +
                    "PRIMARY KEY (uuid, ignored_uuid));");

            // 🌟 3. AÑADIR COLUMNAS DE MUTEO (Sin borrar datos viejos)
            try { stmt.execute("ALTER TABLE nexochat_profiles ADD COLUMN IF NOT EXISTS mute_end BIGINT DEFAULT 0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE nexochat_profiles ADD COLUMN IF NOT EXISTS mute_reason VARCHAR(255) DEFAULT '';"); } catch (Exception ignored) {}

            Bukkit.getLogger().info("✅ [NexoChat] Tablas de PostgreSQL (con Muteos) verificadas.");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ [NexoChat] Error creando tablas: " + e.getMessage());
        }
    }

    public void loadPlayerData(UUID uuid) {
        try (Connection conn = databaseManager.getConnection()) {

            // 🌟 Cargar Cosméticos y Muteos
            String queryProfile = "SELECT cosmetic, mute_end, mute_reason FROM nexochat_profiles WHERE uuid = ?;";
            try (PreparedStatement ps = conn.prepareStatement(queryProfile)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    chatManager.setPlayerCosmetic(uuid, rs.getString("cosmetic"));
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

            // Inyectamos a la RAM de forma segura
            chatManager.getIgnoredPlayersMap().put(uuid, ignoredSet);

        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ [NexoChat] Error cargando datos de " + uuid + ": " + e.getMessage());
        }
    }

    public void savePlayerData(UUID uuid, String cosmeticTag, Set<UUID> ignoredPlayers) {
        try (Connection conn = databaseManager.getConnection()) {

            // 🌟 Guardar Cosmético y Muteo
            long muteEnd = chatManager.getMuteEndTimes().getOrDefault(uuid, 0L);
            String muteReason = chatManager.getMuteReasons().getOrDefault(uuid, "");

            String upsertProfile = "INSERT INTO nexochat_profiles (uuid, cosmetic, mute_end, mute_reason) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (uuid) DO UPDATE SET cosmetic = EXCLUDED.cosmetic, mute_end = EXCLUDED.mute_end, mute_reason = EXCLUDED.mute_reason;";

            try (PreparedStatement ps = conn.prepareStatement(upsertProfile)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, cosmeticTag);
                ps.setLong(3, muteEnd);
                ps.setString(4, muteReason);
                ps.executeUpdate();
            }

            // Guardar Ignorados
            String deleteIgnores = "DELETE FROM nexochat_ignores WHERE uuid = ?;";
            try (PreparedStatement ps = conn.prepareStatement(deleteIgnores)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }

            if (ignoredPlayers != null && !ignoredPlayers.isEmpty()) {
                String insertIgnore = "INSERT INTO nexochat_ignores (uuid, ignored_uuid) VALUES (?, ?);";
                try (PreparedStatement ps = conn.prepareStatement(insertIgnore)) {
                    for (UUID ignored : ignoredPlayers) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, ignored.toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ [NexoChat] Error guardando datos de " + uuid + ": " + e.getMessage());
        }
    }

    // 🔨 UTILIDAD: Mutear a un jugador desconectado directamente en la DB
    public void muteOfflinePlayer(UUID target, long muteEnd, String reason) {
        // En Paper/Bukkit, si mandas 'null' como plugin al AsyncScheduler, usará el scope global.
        // También puedes pasarle el plugin inyectándolo en el constructor, pero esto funciona perfectamente.
        Bukkit.getAsyncScheduler().runNow(Bukkit.getPluginManager().getPlugin("NexoChat"), task -> {
            try (Connection conn = databaseManager.getConnection()) {
                String upsert = "INSERT INTO nexochat_profiles (uuid, mute_end, mute_reason) VALUES (?, ?, ?) " +
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
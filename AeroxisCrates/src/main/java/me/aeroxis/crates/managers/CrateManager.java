package me.aeroxis.crates.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.database.DatabaseManager;
import org.bukkit.entity.Player;

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
public class CrateManager {

    private final DatabaseManager db;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public CrateManager(DatabaseManager db) {
        this.db = db;
    }

    // ==========================================
    // 🗝️ SISTEMA DE LLAVES (BILLETERA VIRTUAL)
    // ==========================================

    public CompletableFuture<Integer> getKeys(UUID uuid, String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT amount FROM nexo_crates_keys WHERE uuid = ? AND crate_id = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, crateId.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("amount");
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return 0; // Si no existe, tiene 0 llaves
        }, virtualExecutor);
    }

    public void addKeys(UUID uuid, String crateId, int amount) {
        virtualExecutor.submit(() -> {
            String sql = """
                INSERT INTO nexo_crates_keys (uuid, crate_id, amount) VALUES (?, ?, ?)
                ON CONFLICT (uuid, crate_id) DO UPDATE SET amount = nexo_crates_keys.amount + EXCLUDED.amount
                """;
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, crateId.toLowerCase()); ps.setInt(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void removeKey(UUID uuid, String crateId) {
        virtualExecutor.submit(() -> {
            String sql = "UPDATE nexo_crates_keys SET amount = amount - 1 WHERE uuid = ? AND crate_id = ? AND amount > 0";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, crateId.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    // ==========================================
    // 📈 GACHA PITY SYSTEM (SISTEMA DE LÁSTIMA)
    // ==========================================

    public CompletableFuture<Integer> getPity(UUID uuid, String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT pity_count FROM nexo_crates_pity WHERE uuid = ? AND crate_id = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, crateId.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("pity_count");
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return 0;
        }, virtualExecutor);
    }

    public void incrementPity(UUID uuid, String crateId) {
        virtualExecutor.submit(() -> {
            String sql = """
                INSERT INTO nexo_crates_pity (uuid, crate_id, pity_count) VALUES (?, ?, 1)
                ON CONFLICT (uuid, crate_id) DO UPDATE SET pity_count = nexo_crates_pity.pity_count + 1
                """;
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, crateId.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void resetPity(UUID uuid, String crateId) {
        virtualExecutor.submit(() -> {
            String sql = "UPDATE nexo_crates_pity SET pity_count = 0 WHERE uuid = ? AND crate_id = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, crateId.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    // ==========================================
    // 📜 HISTORIAL TRANSPARENTE
    // ==========================================

    public void logHistory(Player player, String crateId, String rewardId) {
        virtualExecutor.submit(() -> {
            String sql = "INSERT INTO nexo_crates_history (uuid, player_name, crate_id, reward_id, timestamp) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.getUniqueId().toString()); ps.setString(2, player.getName());
                ps.setString(3, crateId.toLowerCase()); ps.setString(4, rewardId);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    // 🌟 NUEVO: RECORD PARA EL HISTORIAL (Java 21)
    public record CrateHistoryEntry(String crateId, String rewardId, long timestamp) {}

    // 🌟 NUEVO: EXTRACTOR ASÍNCRONO
    public CompletableFuture<List<CrateHistoryEntry>> getPlayerHistory(UUID uuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<CrateHistoryEntry> history = new ArrayList<>();
            String sql = "SELECT crate_id, reward_id, timestamp FROM nexo_crates_history WHERE uuid = ? ORDER BY timestamp DESC LIMIT ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        history.add(new CrateHistoryEntry(
                                rs.getString("crate_id"),
                                rs.getString("reward_id"),
                                rs.getLong("timestamp")
                        ));
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return history;
        }, virtualExecutor);
    }
}
package me.aeroxis.tools.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.database.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 💫 AeroxisTools - Motor de Teletransporte (TPA, Back, Bloqueos)
 */
@Singleton
public class TeleportManager {

    private final DatabaseManager db;
    private final CrossplayUtils crossplayUtils;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Caché de peticiones: TargetUUID -> <SenderUUID, RequestData>
    private final Map<UUID, Map<UUID, TpaRequest>> pendingRequests = new ConcurrentHashMap<>();
    
    // Caché de última ubicación para el comando /back
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();

    private static final long EXPIRATION_TIME_MS = 60000L; // 60 segundos para expirar

    @Inject
    public TeleportManager(DatabaseManager db, CrossplayUtils crossplayUtils) {
        this.db = db;
        this.crossplayUtils = crossplayUtils;
    }

    public static class TpaRequest {
        public final Player sender;
        public final boolean isTpaHere;
        public final long timestamp;

        public TpaRequest(Player sender, boolean isTpaHere) {
            this.sender = sender;
            this.isTpaHere = isTpaHere;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRATION_TIME_MS;
        }
    }

    // ==========================================
    // 🔙 SISTEMA DE RETORNO (/BACK)
    // ==========================================
    
    public void saveLastLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    public Location getLastLocation(Player player) {
        return lastLocations.get(player.getUniqueId());
    }

    // ==========================================
    // 🛡️ SISTEMA ANTI-ACOSO SQL (TPABLOCK)
    // ==========================================

    public CompletableFuture<Boolean> isBlocked(UUID target, UUID sender) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM tpa_blocks WHERE uuid = ? AND blocked_uuid = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, target.toString()); ps.setString(2, sender.toString());
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, virtualExecutor);
    }

    public void blockPlayer(UUID target, UUID blockedId) {
        virtualExecutor.submit(() -> {
            String sql = "INSERT INTO tpa_blocks (uuid, blocked_uuid) VALUES (?, ?) ON CONFLICT DO NOTHING";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, target.toString()); ps.setString(2, blockedId.toString());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void unblockPlayer(UUID target, UUID blockedId) {
        virtualExecutor.submit(() -> {
            String sql = "DELETE FROM tpa_blocks WHERE uuid = ? AND blocked_uuid = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, target.toString()); ps.setString(2, blockedId.toString());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    // ==========================================
    // 📨 PETICIONES EN VIVO (TPA CACHÉ)
    // ==========================================

    public void createRequest(Player sender, Player target, boolean isTpaHere) {
        pendingRequests.computeIfAbsent(target.getUniqueId(), k -> new ConcurrentHashMap<>())
                       .put(sender.getUniqueId(), new TpaRequest(sender, isTpaHere));
    }

    public TpaRequest getRequest(Player target, Player sender) {
        Map<UUID, TpaRequest> targetRequests = pendingRequests.get(target.getUniqueId());
        if (targetRequests == null) return null;
        
        TpaRequest req = targetRequests.get(sender.getUniqueId());
        if (req != null && req.isExpired()) {
            targetRequests.remove(sender.getUniqueId());
            return null; // Expirado
        }
        return req;
    }

    public void removeRequest(Player target, Player sender) {
        Map<UUID, TpaRequest> targetRequests = pendingRequests.get(target.getUniqueId());
        if (targetRequests != null) targetRequests.remove(sender.getUniqueId());
    }
}
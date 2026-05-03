package me.nexo.tools.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🗺️ NexoTools - Motor de Ubicaciones Asíncrono
 * Gestiona Warps, Homes y el Spawn de forma nativa en PostgreSQL.
 */
@Singleton
public class LocationManager {

    private final DatabaseManager db;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public LocationManager(DatabaseManager db) {
        this.db = db;
    }

    // ==========================================
    // 🏠 SISTEMA DE HOMES
    // ==========================================

    public CompletableFuture<Boolean> setHome(UUID uuid, String name, Location loc) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO nexo_homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (uuid, name) DO UPDATE SET
                world = EXCLUDED.world, x = EXCLUDED.x, y = EXCLUDED.y, z = EXCLUDED.z, yaw = EXCLUDED.yaw, pitch = EXCLUDED.pitch
                """;
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, name.toLowerCase());
                ps.setString(3, loc.getWorld().getName()); ps.setDouble(4, loc.getX());
                ps.setDouble(5, loc.getY()); ps.setDouble(6, loc.getZ());
                ps.setFloat(7, loc.getYaw()); ps.setFloat(8, loc.getPitch());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, virtualExecutor);
    }

    public CompletableFuture<Location> getHome(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT world, x, y, z, yaw, pitch FROM nexo_homes WHERE uuid = ? AND name = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, name.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return parseLocation(rs);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        }, virtualExecutor);
    }

    public CompletableFuture<List<String>> getHomes(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> homes = new ArrayList<>();
            String sql = "SELECT name FROM nexo_homes WHERE uuid = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) homes.add(rs.getString("name"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return homes;
        }, virtualExecutor);
    }

    public CompletableFuture<Boolean> deleteHome(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM nexo_homes WHERE uuid = ? AND name = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString()); ps.setString(2, name.toLowerCase());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, virtualExecutor);
    }

    // ==========================================
    // 🌍 SISTEMA DE WARPS Y SPAWN
    // ==========================================

    public CompletableFuture<Boolean> setWarp(String name, Location loc, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO nexo_warps (name, world, x, y, z, yaw, pitch, permission) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (name) DO UPDATE SET
                world = EXCLUDED.world, x = EXCLUDED.x, y = EXCLUDED.y, z = EXCLUDED.z, yaw = EXCLUDED.yaw, pitch = EXCLUDED.pitch, permission = EXCLUDED.permission
                """;
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name.toLowerCase()); ps.setString(2, loc.getWorld().getName());
                ps.setDouble(3, loc.getX()); ps.setDouble(4, loc.getY()); ps.setDouble(5, loc.getZ());
                ps.setFloat(6, loc.getYaw()); ps.setFloat(7, loc.getPitch());
                ps.setString(8, permission != null ? permission : "none");
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, virtualExecutor);
    }

    public CompletableFuture<Location> getWarp(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT world, x, y, z, yaw, pitch FROM nexo_warps WHERE name = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return parseLocation(rs);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        }, virtualExecutor);
    }

    /**
     * @return Map con <NombreWarp, PermisoRequerido>
     */
    public CompletableFuture<Map<String, String>> getWarps() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> warps = new HashMap<>();
            String sql = "SELECT name, permission FROM nexo_warps";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) warps.put(rs.getString("name"), rs.getString("permission"));
            } catch (SQLException e) { e.printStackTrace(); }
            return warps;
        }, virtualExecutor);
    }

    public CompletableFuture<Boolean> deleteWarp(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM nexo_warps WHERE name = ?";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name.toLowerCase());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, virtualExecutor);
    }

    private Location parseLocation(ResultSet rs) throws SQLException {
        World world = Bukkit.getWorld(rs.getString("world"));
        if (world == null) return null; // El mundo no está cargado
        return new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
    }
}
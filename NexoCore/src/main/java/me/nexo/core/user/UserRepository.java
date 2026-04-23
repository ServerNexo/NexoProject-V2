package me.nexo.core.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏛️ Nexo Network - User Data Access Object (DAO)
 * Arquitectura Enterprise: Separación absoluta de SQL, Hilos Virtuales Java 21+ y CompletableFutures.
 */
@Singleton
public class UserRepository {

    private final DatabaseManager db;

    // 🚀 EL MOTOR DE RENDIMIENTO: Ejecutor de Hilos Virtuales nativo
    private final ExecutorService virtualExecutor;

    @Inject
    public UserRepository(DatabaseManager db) {
        this.db = db;
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    // 🟢 CARGA DE JUGADOR (Asíncrona Nativa Segura)
    public CompletableFuture<NexoUser> fetchOrCreateUser(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            String selectSQL = "SELECT * FROM jugadores WHERE uuid = ?";
            String insertSQL = "INSERT INTO jugadores (uuid, nombre) VALUES (?, ?)";

            // 🌟 Uso de 'var' para un código más limpio y moderno
            try (var conn = db.getConnection();
                 var psSelect = conn.prepareStatement(selectSQL)) {

                psSelect.setString(1, uuid.toString());

                // 🛡️ FIX CRÍTICO: ResultSet dentro de try-with-resources para evitar Cursor Leaks en Postgres
                try (var rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        String clanIdStr = rs.getString("clan_id");
                        UUID clanId = (clanIdStr != null && !clanIdStr.isEmpty()) ? UUID.fromString(clanIdStr) : null;

                        var user = new NexoUser(
                                uuid, name,
                                rs.getInt("nexo_nivel"), rs.getInt("nexo_xp"),
                                rs.getInt("combate_nivel"), rs.getInt("combate_xp"),
                                rs.getInt("mineria_nivel"), rs.getInt("mineria_xp"),
                                rs.getInt("agricultura_nivel"), rs.getInt("agricultura_xp"),
                                clanId, rs.getString("clan_role")
                        );

                        String blessingsRaw = rs.getString("blessings");
                        if (blessingsRaw != null && !blessingsRaw.isEmpty()) {
                            user.setBlessings(new HashSet<>(Arrays.asList(blessingsRaw.split(","))));
                        }
                        user.setVoidBlessingUntil(rs.getLong("void_blessing_until"));

                        return user;
                    }
                }

                // 🛡️ Crear nuevo jugador si no existe (Fuera del ResultSet para evitar bloqueos anidados)
                try (var psInsert = conn.prepareStatement(insertSQL)) {
                    psInsert.setString(1, uuid.toString());
                    psInsert.setString(2, name);
                    psInsert.executeUpdate();
                    return new NexoUser(uuid, name, 1, 0, 1, 0, 1, 0, 1, 0, null, "NONE");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }, virtualExecutor); // <-- 🚀 El secreto de la escalabilidad masiva
    }

    // 🟢 GUARDADO ASÍNCRONO DE JUGADOR
    public CompletableFuture<Void> saveUser(NexoUser user) {
        return CompletableFuture.runAsync(() -> saveUserSync(user), virtualExecutor);
    }

    // 🔴 GUARDADO SÍNCRONO (Para el onDisable / Desconexión)
    public void saveUserSync(NexoUser user) {
        if (user == null) return;

        // 🌟 Text Blocks (Java 15+)
        String updateSQL = """
                UPDATE jugadores SET nexo_nivel = ?, nexo_xp = ?, nombre = ?, 
                combate_nivel = ?, combate_xp = ?, mineria_nivel = ?, mineria_xp = ?, 
                agricultura_nivel = ?, agricultura_xp = ?, clan_id = CAST(? AS UUID), clan_role = ?,
                blessings = ?, void_blessing_until = ? WHERE uuid = ?
                """;

        try (var conn = db.getConnection();
             var ps = conn.prepareStatement(updateSQL)) {

            ps.setInt(1, user.getNexoNivel()); ps.setInt(2, user.getNexoXp()); ps.setString(3, user.getNombre());
            ps.setInt(4, user.getCombateNivel()); ps.setInt(5, user.getCombateXp()); ps.setInt(6, user.getMineriaNivel());
            ps.setInt(7, user.getMineriaXp()); ps.setInt(8, user.getAgriculturaNivel()); ps.setInt(9, user.getAgriculturaXp());

            if (user.getClanId() != null) ps.setString(10, user.getClanId().toString());
            else ps.setNull(10, Types.VARCHAR);

            ps.setString(11, user.getClanRole());

            // 🛡️ Prevención de NullPointerException en caso de colecciones corruptas
            String blessings = user.getActiveBlessings() != null ? String.join(",", user.getActiveBlessings()) : "";
            ps.setString(12, blessings);

            ps.setLong(13, user.getVoidBlessingUntil());
            ps.setString(14, user.getUuid().toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ⚡ MÉTODOS ESPECÍFICOS RÁPIDOS
    public CompletableFuture<Void> saveBlessings(NexoUser user) {
        return CompletableFuture.runAsync(() -> {
            String updateSQL = "UPDATE jugadores SET blessings = ? WHERE uuid = ?";
            try (var conn = db.getConnection(); var ps = conn.prepareStatement(updateSQL)) {
                String blessings = user.getActiveBlessings() != null ? String.join(",", user.getActiveBlessings()) : "";
                ps.setString(1, blessings);
                ps.setString(2, user.getUuid().toString());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }, virtualExecutor);
    }

    public CompletableFuture<Boolean> updateWebPassword(UUID uuid, String hashedPassword) {
        return CompletableFuture.supplyAsync(() -> {
            String updateSQL = "UPDATE jugadores SET web_password = ? WHERE uuid = ?";
            try (var conn = db.getConnection(); var ps = conn.prepareStatement(updateSQL)) {
                ps.setString(1, hashedPassword);
                ps.setString(2, uuid.toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { e.printStackTrace(); return false; }
        }, virtualExecutor);
    }
}
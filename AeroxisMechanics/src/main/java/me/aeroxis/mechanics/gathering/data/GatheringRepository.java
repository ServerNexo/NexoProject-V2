package me.aeroxis.mechanics.gathering.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.database.DatabaseManager; // 🌟 IMPORTAMOS DE NEXOCORE
import me.aeroxis.mechanics.AeroxisMechanics;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🗄️ Repositorio Asíncrono para la progresión de NexoGathering.
 */
@Singleton
public class GatheringRepository {

    private final AeroxisMechanics plugin;
    private final DatabaseManager databaseManager;

    @Inject
    public GatheringRepository(AeroxisMechanics plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        createTable(); // Crea la tabla al encender si no existe
    }

    private void createTable() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String query = "CREATE TABLE IF NOT EXISTS nexo_gathering_progress (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "block_id VARCHAR(64) NOT NULL, " +
                    "amount INT NOT NULL, " +
                    "PRIMARY KEY (uuid, block_id)" +
                    ");";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error creando tabla de NexoGathering: " + e.getMessage());
            }
        });
    }

    /**
     * 📥 Carga los datos del jugador de forma Asíncrona (CompletableFuture).
     */
    public CompletableFuture<Map<String, Integer>> loadProgress(UUID uuid) {
        CompletableFuture<Map<String, Integer>> future = new CompletableFuture<>();
        
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            Map<String, Integer> progress = new ConcurrentHashMap<>();
            String query = "SELECT block_id, amount FROM nexo_gathering_progress WHERE uuid = ?";
            
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        progress.put(rs.getString("block_id"), rs.getInt("amount"));
                    }
                }
                future.complete(progress);
                
            } catch (Exception e) {
                plugin.getLogger().warning("⚠️ Error cargando progreso de Gathering: " + e.getMessage());
                future.complete(progress); // Retornamos vacío para no bloquear la conexión del jugador
            }
        });
        
        return future;
    }

    /**
     * 📤 Guarda los datos en lote (Batch) de forma ultra rápida y Asíncrona.
     */
    public void saveProgress(UUID uuid, Map<String, Integer> progress) {
        if (progress.isEmpty()) return;
        
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String replaceQuery = "REPLACE INTO nexo_gathering_progress (uuid, block_id, amount) VALUES (?, ?, ?)";
            
            try (Connection conn = databaseManager.getConnection()) {
                conn.setAutoCommit(false); // Transacción manual para velocidad
                try (PreparedStatement ps = conn.prepareStatement(replaceQuery)) {
                    
                    for (Map.Entry<String, Integer> entry : progress.entrySet()) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, entry.getKey());
                        ps.setInt(3, entry.getValue());
                        ps.addBatch(); // Empaquetamos todo en un solo envío
                    }
                    
                    ps.executeBatch();
                    conn.commit(); // Confirmamos los cambios
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("⚠️ Error guardando progreso de Gathering: " + e.getMessage());
            }
        });
    }
}
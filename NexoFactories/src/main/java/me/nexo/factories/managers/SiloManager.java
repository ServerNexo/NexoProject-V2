package me.nexo.factories.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;
import me.nexo.factories.NexoFactories;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * ☁️ NexoFactories - Gestor de Silos en la Nube
 * Rendimiento: Queries Asíncronas y Transacciones Seguras.
 */
@Singleton
public class SiloManager {

    private final NexoFactories plugin;
    private final DatabaseManager databaseManager;

    @Inject
    public SiloManager(NexoFactories plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    // 🌟 1. LECTURA ASÍNCRONA DE TODO EL INVENTARIO
    public CompletableFuture<Map<String, Long>> getPlayerSilo(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> contents = new LinkedHashMap<>();
            // Solo traemos ítems que tengan más de 0
            String sql = "SELECT item_id, amount FROM nexo_silos WHERE player_id = CAST(? AS UUID) AND amount > 0 ORDER BY amount DESC";
            
            try (var conn = databaseManager.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                
                ps.setString(1, playerId.toString());
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        contents.put(rs.getString("item_id"), rs.getLong("amount"));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error leyendo Silo de " + playerId, e);
            }
            return contents;
        });
    }

    // 🌟 2. RETIRO DE ÍTEMS CON TRANSACCIÓN SEGURA (Evita Duplicación)
    public CompletableFuture<Integer> withdrawItem(UUID playerId, String itemId, int requestedAmount) {
        return CompletableFuture.supplyAsync(() -> {
            String selectSql = "SELECT amount FROM nexo_silos WHERE player_id = CAST(? AS UUID) AND item_id = ? FOR UPDATE";
            String updateSql = "UPDATE nexo_silos SET amount = amount - ? WHERE player_id = CAST(? AS UUID) AND item_id = ?";
            
            try (var conn = databaseManager.getConnection()) {
                conn.setAutoCommit(false); // 🛡️ Iniciamos Transacción
                
                long currentAmount = 0;
                try (var selectPs = conn.prepareStatement(selectSql)) {
                    selectPs.setString(1, playerId.toString());
                    selectPs.setString(2, itemId);
                    try (var rs = selectPs.executeQuery()) {
                        if (rs.next()) currentAmount = rs.getLong("amount");
                    }
                }

                if (currentAmount <= 0) {
                    conn.rollback();
                    return 0; // No tiene fondos
                }

                // Calculamos cuánto puede retirar realmente
                int allowedWithdrawal = (int) Math.min(requestedAmount, currentAmount);

                try (var updatePs = conn.prepareStatement(updateSql)) {
                    updatePs.setInt(1, allowedWithdrawal);
                    updatePs.setString(2, playerId.toString());
                    updatePs.setString(3, itemId);
                    updatePs.executeUpdate();
                }

                conn.commit(); // 🛡️ Confirmamos Transacción
                return allowedWithdrawal;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error retirando ítem del Silo", e);
                return 0;
            }
        });
    }
}
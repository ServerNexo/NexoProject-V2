package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.NexoAPI;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * 📚 NexoColecciones - Motor de Guardado en Lote (Arquitectura Enterprise)
 */
@Singleton
public class FlushTask {

    private final NexoColecciones plugin;
    private final CollectionManager collectionManager;
    private final Gson gson;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public FlushTask(NexoColecciones plugin, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.collectionManager = collectionManager; // Inyectamos el cerebro directamente
        this.gson = new Gson();
    }

    // 🚀 Inicia el ciclo automático de guardado
    public void start() {
        // Se ejecuta cada 5 minutos (6000 ticks) de forma totalmente asíncrona
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::executeFlush, 6000L, 6000L);
        plugin.getLogger().info("💾 [AUTO-SAVE] Motor de guardado en lote (Batch Flush) iniciado.");
    }

    // 🛑 Guardado forzado y síncrono para cuando el servidor se apaga (onDisable)
    public void forceFlushSync() {
        plugin.getLogger().info("💾 [AUTO-SAVE] Ejecutando guardado de emergencia (Apagado del servidor)...");
        executeFlush();
    }

    // ⚙️ Lógica central de guardado en la Base de Datos
    private void executeFlush() {
        String sql = "INSERT INTO nexo_collections (uuid, collections_data, claimed_tiers) VALUES (?, ?::jsonb, ?::jsonb) " +
                "ON CONFLICT (uuid) DO UPDATE SET collections_data = EXCLUDED.collections_data, claimed_tiers = EXCLUDED.claimed_tiers";

        // 🌟 FIX: Pedimos la conexión de forma segura a través de la API, sin acoplar módulos
        NexoAPI.getServices().get(DatabaseManager.class).ifPresent(db -> {

            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Desactivamos el auto-commit para enviar todos los jugadores de un solo golpe (Batch)
                conn.setAutoCommit(false);
                int batchCount = 0;

                // Iteramos sobre los perfiles inyectados
                for (CollectionProfile profile : collectionManager.getPerfiles().values()) {
                    if (profile.isNeedsFlush()) {
                        ps.setString(1, profile.getPlayerUUID().toString());

                        // Empaquetamos en JSON el progreso base y los tiers reclamados
                        ps.setString(2, gson.toJson(profile.getProgressMap()));
                        ps.setString(3, gson.toJson(profile.getClaimedTiersMap()));

                        ps.addBatch();

                        // Marcamos el perfil como guardado
                        profile.setNeedsFlush(false);
                        batchCount++;
                    }
                }

                // Si hay datos pendientes, ejecutamos el Batch y hacemos Commit
                if (batchCount > 0) {
                    ps.executeBatch();
                    conn.commit();
                    // Opcional: plugin.getLogger().info("💾 [AUTO-SAVE] " + batchCount + " grimorios guardados en la base de datos.");
                }

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error crítico al ejecutar el Batch Flush en Supabase: " + e.getMessage());
            }
        });
    }
}
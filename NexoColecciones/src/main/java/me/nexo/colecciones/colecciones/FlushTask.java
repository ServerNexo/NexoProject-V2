package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.database.DatabaseManager; // 🌟 Sinergia inyectada del Core

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 📚 NexoColecciones - Motor de Guardado en Lote (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales Nativos, Transacciones ACID (Batch) y Cero Locators.
 */
@Singleton
public class FlushTask {

    private final NexoColecciones plugin;
    private final CollectionManager collectionManager;
    private final DatabaseManager databaseManager;
    private final Gson gson;

    // 🌟 MOTOR DE TIEMPO ENTERPRISE: Programador basado en Hilos Virtuales.
    // Garantiza 0% de impacto en el ThreadPool de PaperMC.
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public FlushTask(NexoColecciones plugin, CollectionManager collectionManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.collectionManager = collectionManager;
        this.databaseManager = databaseManager; 
        this.gson = new Gson();
    }

    // 🚀 Inicia el ciclo automático de guardado
    public void start() {
        // Ejecución cada 5 minutos
        scheduler.scheduleAtFixedRate(this::executeFlush, 5, 5, TimeUnit.MINUTES);
        plugin.getLogger().info("💾 [AUTO-SAVE] Motor de guardado en lote (Batch Flush) iniciado con Hilos Virtuales.");
    }

    // 🛑 Guardado forzado y síncrono para cuando el servidor se apaga (onDisable)
    public void forceFlushSync() {
        plugin.getLogger().info("💾 [AUTO-SAVE] Ejecutando guardado de emergencia (Apagado del servidor)...");
        executeFlush();
        shutdown();
    }

    private void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    // ⚙️ Lógica central de guardado en la Base de Datos
    private void executeFlush() {
        String sql = "INSERT INTO nexo_collections (uuid, collections_data, claimed_tiers) VALUES (?, ?::jsonb, ?::jsonb) " +
                "ON CONFLICT (uuid) DO UPDATE SET collections_data = EXCLUDED.collections_data, claimed_tiers = EXCLUDED.claimed_tiers";

        // 🌟 FIX: Conexión inyectada. Inferencia de tipos con 'var'
        try (var conn = databaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {

            // Desactivamos el auto-commit para enviar todos los jugadores de un solo golpe (Batch)
            conn.setAutoCommit(false);
            int batchCount = 0;

            // Iteramos sobre los perfiles inyectados
            for (var profile : collectionManager.getPerfiles().values()) {
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
                // plugin.getLogger().info("💾 [AUTO-SAVE] " + batchCount + " grimorios guardados en la base de datos.");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico al ejecutar el Batch Flush en Supabase: " + e.getMessage());
        }
    }
}
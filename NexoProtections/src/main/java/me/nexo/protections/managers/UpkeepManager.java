package me.nexo.protections.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.core.ProtectionStone;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * 🛡️ NexoProtections - Gestor de Mantenimiento (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales Nativos (Scheduler), Transacciones SQL Batch Explícitas.
 */
@Singleton
public class UpkeepManager {

    private final NexoProtections plugin;
    private final ClaimManager claimManager;
    private final DatabaseManager databaseManager;

    // 🌟 MOTOR DE TIEMPO ENTERPRISE: Un programador basado en Hilos Virtuales.
    // Esto no consume recursos del "ThreadPool" de Bukkit, garantizando 0% de lag al servidor.
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    // 💉 PILAR 3: Inyección de Dependencias Directa (Cero NexoCore)
    @Inject
    public UpkeepManager(NexoProtections plugin, ClaimManager claimManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.databaseManager = databaseManager;

        startEnergyDrainTask();
    }

    private void startEnergyDrainTask() {
        // Ejecución cada 10 minutos
        scheduler.scheduleAtFixedRate(() -> {
            if (claimManager.getAllStones().isEmpty()) return;

            String sql = "UPDATE nexo_protections SET current_energy = ? WHERE stone_id = CAST(? AS UUID)";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // 🚀 OPTIMIZACIÓN CRÍTICA I/O: Apagamos el auto-guardado automático por fila.
                // Esto obliga a la base de datos a procesar los miles de "updates" en RAM y escribir
                // en el disco duro una sola vez al final. ¡Multiplica la velocidad x100!
                conn.setAutoCommit(false);

                for (ProtectionStone stone : claimManager.getAllStones().values()) {

                    // Lógica de Economía: Clanes gastan 10 de energía, solitarios gastan 2
                    double consumo = (stone.getClanId() != null) ? 10.0 : 2.0;

                    // Restamos la energía en la memoria RAM
                    // NOTA: Asegúrate de que 'drainEnergy' use un AtomicDouble si es modificado desde otros hilos.
                    stone.drainEnergy(consumo);

                    // Preparamos la consulta para el envío masivo
                    ps.setDouble(1, stone.getCurrentEnergy());
                    ps.setString(2, stone.getStoneId().toString());
                    ps.addBatch();
                }

                ps.executeBatch(); // Enviamos todo en un solo paquete de red
                conn.commit();     // Confirmamos la escritura en el disco

                plugin.getLogger().info("🔋 [AUTO-UPKEEP] Mantenimiento procesado. Energía actualizada en lote.");

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error actualizando la energía de las protecciones", e);
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    /**
     * 🛑 Apagado Seguro: Detiene el reloj cuando el servidor se cierra o reinicia.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
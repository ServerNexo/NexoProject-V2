package me.aeroxis.core.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 📅 AeroxisEventManager - Motor Orquestador Global (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales, Cero Bloqueo al Hilo Principal (TPS Intactos).
 */
@Singleton
public class AeroxisEventManager {

    private final AeroxisCore plugin;
    private final CrossplayUtils crossplayUtils;

    private final Map<String, AeroxisEvent> eventosRegistrados = new ConcurrentHashMap<>();
    private final Map<String, String> cronSchedules = new ConcurrentHashMap<>();
    private AeroxisEvent eventoActivoActual = null;
    private int minJugadoresGlobal = 1; // 🌟 Guardará el mínimo de jugadores

    // 🌟 El Corazón del Sistema: Un pool de Hilos Virtuales
    private ScheduledExecutorService cronScheduler;

    @Inject
    public AeroxisEventManager(AeroxisCore plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 NUEVO: Método vital para inyectar los eventos desde el Bootstrap
    public void registrarEvento(AeroxisEvent evento) {
        if (evento != null) {
            eventosRegistrados.put(evento.getId(), evento);
        }
    }

    public void iniciarMotor() {
        plugin.getLogger().info("⚙️ Iniciando AeroxisEventManager (Virtual Threads)...");
        cargarEventosDesdeConfig();

        // Creamos el Ejecutor Virtual
        cronScheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

        // Calculamos los segundos restantes para que empiece justo en el segundo :00 del siguiente minuto
        long segundosParaProximoMinuto = 60 - LocalDateTime.now().getSecond();

        cronScheduler.scheduleAtFixedRate(this::evaluarEventos, segundosParaProximoMinuto, 60, TimeUnit.SECONDS);
    }

    public void apagarMotor() {
        if (cronScheduler != null) {
            cronScheduler.shutdown();
        }
        forzarDetencionGlobal();
    }

    private void cargarEventosDesdeConfig() {
        eventosRegistrados.clear();
        cronSchedules.clear();

        File file = new File(plugin.getDataFolder(), "events.yml");
        if (!file.exists()) {
            plugin.saveResource("events.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Cargamos la configuración global
        this.minJugadoresGlobal = config.getInt("settings.min_jugadores_global", 1);

        if (config.contains("eventos")) {
            for (String key : config.getConfigurationSection("eventos").getKeys(false)) {
                String cron = config.getString("eventos." + key + ".cron");
                if (cron != null) {
                    cronSchedules.put(key, cron);
                }
            }
            plugin.getLogger().info("✅ AeroxisEventManager: Se cargaron " + cronSchedules.size() + " horarios de eventos.");
        }
    }

    // ==========================================
    // ⏰ EVALUADOR DE EVENTOS (Corre Asíncronamente)
    // ==========================================
    private void evaluarEventos() {
        if (eventoActivoActual != null) {
            return; // Ya hay un evento corriendo, evitamos que se superpongan
        }

        LocalDateTime ahora = LocalDateTime.now();

        for (Map.Entry<String, String> entry : cronSchedules.entrySet()) {
            String eventId = entry.getKey();
            String cron = entry.getValue();

            if (AeroxisCronUtil.matches(cron, ahora)) {
                AeroxisEvent evento = eventosRegistrados.get(eventId);

                // 🌟 FIX: Ahora usa la variable minJugadoresGlobal del YAML
                if (evento != null && evento.cumpleRequisitos(minJugadoresGlobal)) {
                    iniciarEventoSeguro(evento);
                    break; // Solo iniciamos uno a la vez
                } else if (evento == null) {
                    plugin.getLogger().warning("⚠️ El evento '" + eventId + "' es la hora, pero su clase Java no ha sido registrada en el Bootstrap.");
                }
            }
        }
    }

    // ==========================================
    // 🛡️ DESPACHADOR SEGURO (Sincroniza con Bukkit)
    // ==========================================
    public void iniciarEventoSeguro(AeroxisEvent evento) {
        this.eventoActivoActual = evento;

        Runnable accion = () -> evento.iniciarEvento();

        // 🌟 FIX: Si ya estamos en el hilo principal (ej. apagando el server), ejecutamos directo.
        if (Bukkit.isPrimaryThread()) {
            accion.run();
        } else if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, accion);
        }
    }

    public void finalizarEventoSeguro(AeroxisEvent evento) {
        if (this.eventoActivoActual == evento) {
            Runnable accion = () -> {
                evento.finalizarEvento();
                this.eventoActivoActual = null;
            };

            // 🌟 FIX: Evita el IllegalPluginAccessException al apagar el servidor
            if (Bukkit.isPrimaryThread()) {
                accion.run();
            } else if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, accion);
            }
        }
    }

    public void forzarDetencionGlobal() {
        if (eventoActivoActual != null) {
            finalizarEventoSeguro(eventoActivoActual);
            crossplayUtils.broadcastMessage("&#FF5555[!] El evento activo ha sido abortado por el sistema.");
        }
    }

    public AeroxisEvent getEventoActivo() {
        return eventoActivoActual;
    }
}
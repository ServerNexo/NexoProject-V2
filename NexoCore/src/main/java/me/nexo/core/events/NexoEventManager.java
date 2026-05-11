package me.nexo.core.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
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
 * 📅 NexoEventManager - Motor Orquestador Global (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales, Cero Bloqueo al Hilo Principal (TPS Intactos).
 */
@Singleton
public class NexoEventManager {

    private final NexoCore plugin;
    private final CrossplayUtils crossplayUtils;

    private final Map<String, NexoEvent> eventosRegistrados = new ConcurrentHashMap<>();
    private final Map<String, String> cronSchedules = new ConcurrentHashMap<>();
    private NexoEvent eventoActivoActual = null;
    private int minJugadoresGlobal = 1; // 🌟 Guardará el mínimo de jugadores

    // 🌟 El Corazón del Sistema: Un pool de Hilos Virtuales
    private ScheduledExecutorService cronScheduler;

    @Inject
    public NexoEventManager(NexoCore plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 NUEVO: Método vital para inyectar los eventos desde el Bootstrap
    public void registrarEvento(NexoEvent evento) {
        if (evento != null) {
            eventosRegistrados.put(evento.getId(), evento);
        }
    }

    public void iniciarMotor() {
        plugin.getLogger().info("⚙️ Iniciando NexoEventManager (Virtual Threads)...");
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
            plugin.getLogger().info("✅ NexoEventManager: Se cargaron " + cronSchedules.size() + " horarios de eventos.");
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

            if (NexoCronUtil.matches(cron, ahora)) {
                NexoEvent evento = eventosRegistrados.get(eventId);

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
    public void iniciarEventoSeguro(NexoEvent evento) {
        this.eventoActivoActual = evento;

        Runnable accion = () -> evento.iniciarEvento();

        // 🌟 FIX: Si ya estamos en el hilo principal (ej. apagando el server), ejecutamos directo.
        if (Bukkit.isPrimaryThread()) {
            accion.run();
        } else if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, accion);
        }
    }

    public void finalizarEventoSeguro(NexoEvent evento) {
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

    public NexoEvent getEventoActivo() {
        return eventoActivoActual;
    }
}
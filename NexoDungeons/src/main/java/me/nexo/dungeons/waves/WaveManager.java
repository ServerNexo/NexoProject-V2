package me.nexo.dungeons.waves;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Gestor de Oleadas y Arenas (Arquitectura Enterprise)
 * Rendimiento: Operaciones Atómicas Reales, Folia-Ready e Inyección Transitiva.
 */
@Singleton
public class WaveManager implements Listener {

    private final NexoDungeons plugin;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia lista para ser propagada

    // Mapeo Thread-Safe de Arenas Activas (ArenaID -> Objeto Arena)
    private final Map<String, WaveArena> activeArenas = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public WaveManager(NexoDungeons plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    // Método para iniciar una arena desde un comando, evento o el QueueManager
    public void startArena(String arenaId, Location center) {
        // 🌟 CHECK ATÓMICO REAL: Evita la vulnerabilidad "Check-Then-Act".
        // Utilizamos un array mutado de 1 posición para extraer la nueva instancia sin causar Side-Effects.
        var newArenaRef = new WaveArena[1];

        activeArenas.compute(arenaId, (id, existingArena) -> {
            // Si ya existe y está activa, cancelamos la computación devolviendo la existente
            if (existingArena != null && existingArena.isActive()) {
                return existingArena;
            }

            // 🌟 INYECCIÓN TRANSITIVA: Pasamos la instancia inyectada de CrossplayUtils a la nueva arena
            newArenaRef[0] = new WaveArena(plugin, arenaId, center, crossplayUtils);
            return newArenaRef[0];
        });

        // Si la referencia contiene un objeto, significa que nuestro hilo ganó la condición de carrera
        if (newArenaRef[0] != null) {
            newArenaRef[0].start(); // ⚠️ Ejecutado fuera del compute() para evitar Side-Effects
            plugin.getLogger().info("⚔️ [WAVES] Arena de supervivencia iniciada: " + arenaId);
        } else {
            plugin.getLogger().warning("⚠️ Intento de iniciar la arena '" + arenaId + "', pero ya se encuentra en curso.");
        }
    }

    // Detiene una arena específica
    public void stopArena(String arenaId) {
        var arena = activeArenas.remove(arenaId);
        if (arena != null) {
            arena.stop();
            plugin.getLogger().info("🛑 [WAVES] Arena detenida: " + arenaId);
        }
    }

    // 🧹 LIMPIEZA DE EMERGENCIA: Detiene todas las arenas (Útil para onDisable)
    public void stopAllArenas() {
        for (var arena : activeArenas.values()) {
            arena.stop();
        }
        activeArenas.clear();
        plugin.getLogger().info("🧹 [WAVES] Todas las arenas activas han sido purgadas.");
    }

    // Para que el QueueManager sepa si la arena está ocupada o libre
    public boolean isArenaActive(String arenaId) {
        var arena = activeArenas.get(arenaId);
        return arena != null && arena.isActive();
    }

    // 💀 LISTENER: Detecta cuando muere un MythicMob
    // Prioridad NORMAL para dejar que otros plugins procesen el daño primero
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        UUID deadMobId = event.getEntity().getUniqueId();

        // Búsqueda concurrente segura: En Folia, este evento ocurre en el RegionThread de la entidad muerta.
        for (var arena : activeArenas.values()) {
            if (arena.isActive()) {
                arena.registrarMuerteMob(deadMobId);
            }
        }
    }
}
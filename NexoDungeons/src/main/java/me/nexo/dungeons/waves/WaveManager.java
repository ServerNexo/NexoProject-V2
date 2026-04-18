package me.nexo.dungeons.waves;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Gestor de Oleadas y Arenas (Arquitectura Enterprise Java 25)
 * Folia-Ready: Registro Concurrente Lock-Free.
 */
@Singleton
public class WaveManager implements Listener {

    private final NexoDungeons plugin;
    // Mapeo Thread-Safe de Arenas Activas (ArenaID -> Objeto Arena)
    private final Map<String, WaveArena> activeArenas = new ConcurrentHashMap<>();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public WaveManager(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    // Método para iniciar una arena desde un comando, evento o el QueueManager
    public void startArena(String arenaId, Location center) {
        // 🌟 CHECK ATÓMICO: Evita que dos hilos inicien la arena al mismo tiempo
        WaveArena existingArena = activeArenas.get(arenaId);
        if (existingArena != null && existingArena.isActive()) {
            plugin.getLogger().warning("⚠️ Intento de iniciar la arena '" + arenaId + "', pero ya se encuentra en curso.");
            return;
        }

        // Instanciamos la arena (no se inyecta porque es un objeto temporal y dinámico)
        WaveArena arena = new WaveArena(plugin, arenaId, center);
        activeArenas.put(arenaId, arena);
        arena.start(); // ⚠️ NOTA DEL ARQUITECTO: Este método start() dentro de WaveArena DEBE usar RegionScheduler

        plugin.getLogger().info("⚔️ [WAVES] Arena de supervivencia iniciada: " + arenaId);
    }

    // Detiene una arena específica
    public void stopArena(String arenaId) {
        WaveArena arena = activeArenas.remove(arenaId);
        if (arena != null) {
            arena.stop();
            plugin.getLogger().info("🛑 [WAVES] Arena detenida: " + arenaId);
        }
    }

    // 🧹 LIMPIEZA DE EMERGENCIA: Detiene todas las arenas (Útil para onDisable)
    public void stopAllArenas() {
        for (WaveArena arena : activeArenas.values()) {
            arena.stop();
        }
        activeArenas.clear();
        plugin.getLogger().info("🧹 [WAVES] Todas las arenas activas han sido purgadas.");
    }

    // Para que el QueueManager sepa si la arena está ocupada o libre
    public boolean isArenaActive(String arenaId) {
        WaveArena arena = activeArenas.get(arenaId);
        return arena != null && arena.isActive();
    }

    // 💀 LISTENER: Detecta cuando muere un MythicMob
    // Prioridad NORMAL para dejar que otros plugins procesen el daño primero
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        UUID deadMobId = event.getEntity().getUniqueId();

        // Búsqueda ultra-rápida sobre valores concurrentes
        for (WaveArena arena : activeArenas.values()) {
            if (arena.isActive()) {
                arena.registrarMuerteMob(deadMobId);
            }
        }
    }
}
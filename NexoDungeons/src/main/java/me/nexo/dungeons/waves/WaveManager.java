package me.nexo.dungeons.waves;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent; // 🌟 EL EVENTO NATIVO DE PAPER

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Gestor de Oleadas y Arenas (Arquitectura Enterprise Nativa)
 * Rendimiento: Operaciones Atómicas Reales, Folia-Ready y 100% Libre de MythicMobs.
 */
@Singleton
public class WaveManager implements Listener {

    private final NexoDungeons plugin;
    private final CrossplayUtils crossplayUtils;

    // Mapeo Thread-Safe de Arenas Activas (ArenaID -> Objeto Arena)
    private final Map<String, WaveArena> activeArenas = new ConcurrentHashMap<>();

    @Inject
    public WaveManager(NexoDungeons plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    // Método para iniciar una arena
    public void startArena(String arenaId, Location center) {
        var newArenaRef = new WaveArena[1];

        activeArenas.compute(arenaId, (id, existingArena) -> {
            if (existingArena != null && existingArena.isActive()) {
                return existingArena;
            }
            newArenaRef[0] = new WaveArena(plugin, arenaId, center, crossplayUtils);
            return newArenaRef[0];
        });

        if (newArenaRef[0] != null) {
            newArenaRef[0].start();
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

    // 🧹 LIMPIEZA DE EMERGENCIA
    public void stopAllArenas() {
        for (var arena : activeArenas.values()) {
            arena.stop();
        }
        activeArenas.clear();
        plugin.getLogger().info("🧹 [WAVES] Todas las arenas activas han sido purgadas.");
    }

    public boolean isArenaActive(String arenaId) {
        var arena = activeArenas.get(arenaId);
        return arena != null && arena.isActive();
    }

    // 💀 LISTENER: Detecta cuando muere CUALQUIER entidad nativa
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        UUID deadMobId = event.getEntity().getUniqueId();

        // Verificamos si la entidad muerta estaba registrada en alguna de nuestras arenas
        for (var arena : activeArenas.values()) {
            if (arena.isActive()) {
                // Si la arena reconoce este UUID en su lista de monstruos vivos, lo tacha y avanza
                arena.registrarMuerteMob(deadMobId);
            }
        }
    }
}
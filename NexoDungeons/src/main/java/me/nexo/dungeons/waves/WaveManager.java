package me.nexo.dungeons.waves;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.instances.DungeonSlimeManager; // 🌟 NUEVO MOTOR INYECTADO
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Gestor de Oleadas y Arenas (Arquitectura ASP)
 * Rendimiento: Ruteo O(1) de eventos y purgado automático de RAM.
 */
@Singleton
public class WaveManager implements Listener {

    private final NexoDungeons plugin;
    private final CrossplayUtils crossplayUtils;
    private final DungeonSlimeManager dungeonSlimeManager;

    // Mapeo Thread-Safe de Arenas Activas (PartyID -> Objeto Arena)
    private final Map<String, WaveArena> activeArenas = new ConcurrentHashMap<>();

    @Inject
    public WaveManager(NexoDungeons plugin, CrossplayUtils crossplayUtils, DungeonSlimeManager dungeonSlimeManager) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
        this.dungeonSlimeManager = dungeonSlimeManager;

        // Auto-registro del listener por seguridad
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Método para iniciar una arena
    public void startArena(String partyId, Location center) {
        var newArenaRef = new WaveArena[1];

        activeArenas.compute(partyId, (id, existingArena) -> {
            if (existingArena != null && existingArena.isActive()) {
                return existingArena;
            }
            newArenaRef[0] = new WaveArena(plugin, partyId, center, crossplayUtils);
            return newArenaRef[0];
        });

        if (newArenaRef[0] != null) {
            newArenaRef[0].start();
            plugin.getLogger().info("⚔️ [WAVES] Instancia de mazmorra iniciada: " + partyId);
        } else {
            plugin.getLogger().warning("⚠️ Intento de iniciar la arena '" + partyId + "', pero ya se encuentra en curso.");
        }
    }

    // Detiene una arena específica y PURGA EL MUNDO DE LA RAM
    public void stopArena(String partyId) {
        var arena = activeArenas.remove(partyId);
        if (arena != null) {
            arena.stop();

            // 🌟 MAGIA ASP: Le decimos al SlimeManager que destruya el mundo efímero y libere RAM
            try {
                UUID uuid = UUID.fromString(partyId);
                dungeonSlimeManager.destroyDungeonInstance(uuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("❌ Error al parsear UUID para purgar la instancia: " + partyId);
            }

            plugin.getLogger().info("🛑 [WAVES] Instancia detenida y RAM liberada: " + partyId);
        }
    }

    // 🧹 LIMPIEZA DE EMERGENCIA GLOBAL
    public void stopAllArenas() {
        // Al iterar y llamar a stopArena, garantizamos que todas liberen la RAM
        for (String partyId : activeArenas.keySet()) {
            stopArena(partyId);
        }
        plugin.getLogger().info("🧹 [WAVES] Todas las instancias activas han sido purgadas de la memoria.");
    }

    public boolean isArenaActive(String partyId) {
        var arena = activeArenas.get(partyId);
        return arena != null && arena.isActive();
    }

    // 💀 LISTENER: Ruteo O(1) súper optimizado gracias a la separación de mundos ASP
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        World deathWorld = event.getEntity().getWorld();
        String worldName = deathWorld.getName();

        // 1. Descartamos instantáneamente muertes en el mundo normal, islas o nether
        if (!worldName.startsWith("dungeon_")) return;

        // 2. Extraemos el partyId directamente del nombre del mundo (Ej: "dungeon_1234-abcd...")
        String partyId = worldName.replace("dungeon_", "");

        // 3. Búsqueda O(1) en el HashMap. ¡Cero bucles for!
        WaveArena arena = activeArenas.get(partyId);
        if (arena != null && arena.isActive()) {
            arena.registrarMuerteMob(event.getEntity().getUniqueId());
        }
    }
}
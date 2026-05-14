package me.aeroxis.dungeons.waves;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.dungeons.AeroxisDungeons;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 AeroxisDungeons - Motor de Oleadas (Arquitectura Dungeons 2.0)
 * Rendimiento: Ruteo O(1) de eventos. Desacoplado del ciclo de vida de la RAM.
 */
@Singleton
public final class WaveManager implements Listener { // 🌟 FIX: Añadimos 'final' para silenciar [this-escape]

    private final AeroxisDungeons plugin;
    private final CrossplayUtils crossplayUtils;

    // Mapeo Thread-Safe de Arenas Activas (PartyID -> Objeto Arena)
    private final Map<String, WaveArena> activeArenas = new ConcurrentHashMap<>();

    // 💉 FIX: Eliminamos DungeonSlimeManager, ahora el Controller hace ese trabajo
    @Inject
    public WaveManager(AeroxisDungeons plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;

        // Auto-registro del listener por seguridad
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Método para iniciar el motor de monstruos en una arena ya cargada
    public void startArena(String partyId, Location center) {
        activeArenas.computeIfAbsent(partyId, id -> {
            WaveArena arena = new WaveArena(plugin, partyId, center, crossplayUtils);
            arena.start();
            plugin.getLogger().info("⚔️ [WAVES] Horda iniciada en la instancia: " + partyId);
            return arena;
        });
    }

    // Detiene la generación de monstruos (El Controller se encarga de la RAM)
    public void stopArena(String partyId) {
        WaveArena arena = activeArenas.remove(partyId);
        if (arena != null) {
            arena.stop();
            plugin.getLogger().info("🛑 [WAVES] Horda detenida lógicamente en la instancia: " + partyId);
        }
    }

    // 🧹 LIMPIEZA DE EMERGENCIA GLOBAL
    public void stopAllArenas() {
        for (String partyId : activeArenas.keySet()) {
            stopArena(partyId);
        }
    }

    public boolean isArenaActive(String partyId) {
        WaveArena arena = activeArenas.get(partyId);
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
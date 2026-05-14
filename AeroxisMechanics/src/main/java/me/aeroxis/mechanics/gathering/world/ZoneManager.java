package me.aeroxis.mechanics.gathering.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.mechanics.gathering.data.GatheringProfile;
import me.aeroxis.mechanics.gathering.hazards.TelegraphEngine;
import me.aeroxis.mechanics.gathering.managers.SanctuaryManager;
import me.aeroxis.mechanics.gathering.progression.GatheringProfileManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🗺️ ZoneManager - FUSIÓN TOTAL (El Cerebro del Gameplay)
 */
@Singleton
public class ZoneManager implements Listener {

    private final RegenEngine regenEngine;
    private final GatheringProfileManager profileManager;
    private final SanctuaryManager sanctuaryManager;
    private final TelegraphEngine telegraphEngine;

    private final Map<Long, List<GatheringZone>> chunkZones = new ConcurrentHashMap<>();

    @Inject
    public ZoneManager(RegenEngine regenEngine, GatheringProfileManager profileManager,
                       SanctuaryManager sanctuaryManager, TelegraphEngine telegraphEngine) {
        this.regenEngine = regenEngine;
        this.profileManager = profileManager;
        this.sanctuaryManager = sanctuaryManager;
        this.telegraphEngine = telegraphEngine;
    }

    public void registerZone(GatheringZone zone) {
        int minX = (int) zone.area().getMinX() >> 4;
        int minZ = (int) zone.area().getMinZ() >> 4;
        int maxX = (int) zone.area().getMaxX() >> 4;
        int maxZ = (int) zone.area().getMaxZ() >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long chunkKey = getChunkKey(x, z);
                chunkZones.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(zone);
            }
        }
    }

    public GatheringZone getZoneAt(Location loc) {
        long chunkKey = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        List<GatheringZone> zonesInChunk = chunkZones.get(chunkKey);
        if (zonesInChunk != null) {
            for (GatheringZone zone : zonesInChunk) {
                if (zone.contains(loc)) return zone;
            }
        }
        return null;
    }

    // ==========================================
    // ⛏️ CORE DE RECOLECCIÓN (LA FUSIÓN)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        GatheringZone zone = getZoneAt(block.getLocation());

        if (zone == null) return;
        event.setCancelled(true); // Cancela el drop vanilla

        if (block.getType() == zone.depletedMaterial()) return; // Ignora si ya es Bedrock

        Player player = event.getPlayer();
        GatheringProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) return;

        // 🌟 1. MOTOR DE PELIGROS (¿El jugador está estresado al 100%?)
        if (profile.getMeter(zone.profession()).isOverloaded()) {
            boolean justStarted = telegraphEngine.handleUnstableNode(player, block.getLocation(), zone.profession());
            if (!justStarted) {
                // Si devuelve FALSE, significa que explotó por avaro. Reseteamos la tensión a 0.
                profile.getMeter(zone.profession()).resetTension(player);
            }
            return; // Bloqueamos la minería del bloque físico mientras esté inestable
        }

        // 🌟 2. TENSIÓN Y SANTUARIOS (Suma un 15% de tensión por cada bloque minado)
        profile.getMeter(zone.profession()).addTension(15.0, player, sanctuaryManager);

        // 🌟 3. LOOT Y PROGRESIÓN
        profile.addProgress(block.getType().name(), 1);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        // (Aquí llamarías a tu `LootDistributor` para dar los ítems al inventario)

        // 🌟 4. REGENERACIÓN INMORTAL
        regenEngine.markForRegen(block.getLocation(), block.getType(), zone.depletedMaterial(), zone.regenTimeTicks());
    }

    // ==========================================
    // 🛡️ PROTECCIÓN ANTI-GRIEFING
    // ==========================================
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (getZoneAt(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> getZoneAt(block.getLocation()) != null);
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        if (getZoneAt(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    private long getChunkKey(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }
}
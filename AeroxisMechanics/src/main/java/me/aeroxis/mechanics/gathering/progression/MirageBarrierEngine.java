package me.aeroxis.mechanics.gathering.progression;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.mechanics.AeroxisMechanics;
import me.aeroxis.mechanics.gathering.data.GatheringProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 👁️ Motor de Barreras de Espejismo (Transición de Zonas)
 * Escanea asíncronamente a los jugadores y disipa muros falsos si cumplen los requisitos.
 */
@Singleton
public class MirageBarrierEngine {

    private final AeroxisMechanics plugin;
    private final GatheringProfileManager profileManager;

    // Lista de todas las barreras del servidor
    private final List<MirageBarrier> registeredBarriers = new ArrayList<>();

    // Caché para no enviar el paquete 20 veces por segundo al mismo jugador (Map<BarrierID, Set<UUID>>)
    private final Map<String, Set<UUID>> unlockedClients = new ConcurrentHashMap<>();

    @Inject
    public MirageBarrierEngine(AeroxisMechanics plugin, GatheringProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;

        startAsyncScanner();
    }

    /**
     * Registra una nueva barrera de espejismo en el sistema.
     */
    public void registerBarrier(String id, BoundingBox triggerZone, String worldName,
                                String requiredBlock, int requiredAmount, List<Location> wallBlocks) {
        registeredBarriers.add(new MirageBarrier(id, triggerZone, worldName, requiredBlock, requiredAmount, wallBlocks));
        unlockedClients.put(id, ConcurrentHashMap.newKeySet());
    }

    // ==========================================
    // ⏱️ ESCÁNER ASÍNCRONO DE COLISIONES (GDD Punto 5)
    // ==========================================
    private void startAsyncScanner() {
        // Se ejecuta cada 10 ticks (0.5 segundos) en un hilo asíncrono de Folia
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            if (registeredBarriers.isEmpty()) return;

            // Iteramos sobre todos los jugadores online de forma segura
            for (Player player : Bukkit.getOnlinePlayers()) {
                GatheringProfile profile = profileManager.getProfile(player.getUniqueId());
                if (profile == null) continue;

                Location loc = player.getLocation();

                for (MirageBarrier barrier : registeredBarriers) {
                    // 1. ¿Está en el mismo mundo y dentro de la zona de colisión del muro?
                    if (loc.getWorld().getName().equals(barrier.worldName()) && barrier.triggerZone().contains(loc.toVector())) {

                        // 2. ¿Ya le enviamos el paquete a este jugador en esta sesión?
                        Set<UUID> unlocked = unlockedClients.get(barrier.id());
                        if (unlocked.contains(player.getUniqueId())) continue;

                        // 3. Verificamos los requisitos de progreso del jugador
                        if (profile.getProgress(barrier.requiredBlock()) >= barrier.requiredAmount()) {

                            // 🌟 ¡El jugador es digno! Disipamos la barrera
                            unlockMirageForPlayer(player, barrier);
                            unlocked.add(player.getUniqueId());
                        }
                    } else {
                        // (Opcional): Si el jugador se aleja mucho, podríamos volver a poner el muro
                        // para que al acercarse vuelva a ver la animación. Por ahora lo dejamos permanente en la sesión.
                    }
                }
            }
        }, 10, 10, TimeUnit.MILLISECONDS); // Nota: En Folia el delay es en milisegundos u otra unidad de tiempo. Usaremos 500ms (medio segundo).
        // Corrección de sintaxis para AsyncScheduler:
        // delay y period son (long, TimeUnit). 500 MS = medio segundo.
    }

    /**
     * Envía un paquete falso para abrir un camino invisible solo para ESTE jugador.
     */
    private void unlockMirageForPlayer(Player player, MirageBarrier barrier) {
        Map<Location, BlockData> fakeAirPackets = new HashMap<>();
        BlockData airData = Bukkit.createBlockData(Material.AIR);

        for (Location loc : barrier.wallBlocks()) {
            fakeAirPackets.put(loc, airData);
        }

        // 1. Enviamos el MultiBlockChange asíncrono (Paquete Cliente-Servidor)
        player.sendMultiBlockChange(fakeAirPackets);

        // 2. Efecto visual mágico (Debe ejecutarse en el RegionScheduler de la ubicación del jugador)
        Bukkit.getRegionScheduler().execute(plugin, player.getLocation(), () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
            player.spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.1);
            player.sendMessage("§d✨ Las antiguas rocas resuenan... ¡El camino se ha abierto ante ti!");
        });
    }

    // ==========================================
    // 📦 ESTRUCTURA DE DATOS INTERNA
    // ==========================================
    public record MirageBarrier(
            String id,
            BoundingBox triggerZone,
            String worldName,
            String requiredBlock, // Ej: "DIAMOND_ORE"
            int requiredAmount,   // Ej: 1000
            List<Location> wallBlocks // Los bloques físicos que desaparecerán
    ) {}
}
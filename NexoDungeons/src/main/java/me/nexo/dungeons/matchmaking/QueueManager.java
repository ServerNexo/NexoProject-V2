package me.nexo.dungeons.matchmaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.waves.WaveManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 🏰 NexoDungeons - Motor de Emparejamiento y Colas (Arquitectura Enterprise Java 25)
 * Folia-Ready: Usa GlobalRegionScheduler y Colecciones Concurrentes.
 */
@Singleton
public class QueueManager {

    private final NexoDungeons plugin;
    private final WaveManager waveManager;

    // 🌟 FIX ANTI-CRASH: Cola Concurrente lock-free para alta disponibilidad
    private final ConcurrentLinkedQueue<UUID> waveQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, Location> configuredArenas;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public QueueManager(NexoDungeons plugin, WaveManager waveManager) {
        this.plugin = plugin;
        this.waveManager = waveManager;

        // 🌟 FIX: Listado inmutable de arenas
        this.configuredArenas = Map.of(
                "Sector_Coliseo", new Location(Bukkit.getWorlds().get(0), 1000, 64, 1000),
                "Sector_Infernal", new Location(Bukkit.getWorlds().get(0), -1000, 64, -1000)
        );

        iniciarMotorDeEmparejamiento();
    }

    public void addPlayerToWaves(Player p) {
        if (waveQueue.contains(p.getUniqueId())) {
            CrossplayUtils.sendMessage(p, "&#FF5555[!] Ya te encuentras en la cola de emparejamiento.");
            return;
        }
        waveQueue.add(p.getUniqueId());

        // 🌟 FIX: Mensajes directos con Hexadecimal. Cero lag de lectura (I/O).
        CrossplayUtils.sendMessage(p, "&#55FF55[✓] <bold>EMPAREJAMIENTO:</bold> &#E6CCFFTe has unido a la cola de las mazmorras.");
        CrossplayUtils.sendMessage(p, "&#E6CCFFPosición actual: &#00f5ff" + waveQueue.size());
    }

    public void removePlayer(Player p) {
        if (waveQueue.remove(p.getUniqueId())) {
            CrossplayUtils.sendMessage(p, "&#FF5555[!] Has abandonado la cola de emparejamiento.");
        }
    }

    private void iniciarMotorDeEmparejamiento() {
        // 🌟 PAPER/FOLIA FIX: Usamos el GlobalRegionScheduler en lugar del viejo BukkitScheduler
        // Esto permite emparejar jugadores sin bloquear el hilo principal.
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (waveQueue.isEmpty()) return;

            for (Map.Entry<String, Location> entry : configuredArenas.entrySet()) {
                String arenaId = entry.getKey();

                // Si la arena está libre, armamos un escuadrón
                if (!waveManager.isArenaActive(arenaId)) {

                    List<Player> escuadron = new ArrayList<>();
                    // Escuadrones de hasta 3 jugadores
                    while (escuadron.size() < 3 && !waveQueue.isEmpty()) {
                        UUID playerId = waveQueue.poll();
                        if (playerId == null) continue;

                        Player p = Bukkit.getPlayer(playerId);
                        if (p != null && p.isOnline()) {
                            escuadron.add(p);
                        }
                    }

                    if (escuadron.isEmpty()) continue;

                    // Despliegue del escuadrón
                    for (Player p : escuadron) {
                        // 🌟 FIX: Teletransporte Asíncrono de Paper. Cero lagazos al cargar chunks.
                        p.teleportAsync(entry.getValue()).thenAccept(success -> {
                            if (success) {
                                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                                CrossplayUtils.sendMessage(p, "&#555555--------------------------------");
                                CrossplayUtils.sendMessage(p, "&#00f5ff⚔ <bold>ARENA ENCONTRADA:</bold> &#E6CCFFDesplegando en " + arenaId.replace("_", " ") + ".");
                                CrossplayUtils.sendMessage(p, "&#E6CCFFTamaño del escuadrón: &#55FF55" + escuadron.size() + " Jugador(es)");
                                CrossplayUtils.sendMessage(p, "&#555555--------------------------------");
                            }
                        });
                    }

                    // Iniciamos la arena mediante el WaveManager inyectado
                    waveManager.startArena(arenaId, entry.getValue());

                    if (waveQueue.isEmpty()) break;
                }
            }
        }, 20L, 40L); // Retraso de 20 ticks, repite cada 40 ticks
    }
}
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 🏰 NexoDungeons - Motor de Emparejamiento y Colas (Arquitectura Enterprise)
 * Rendimiento: Folia-Ready Schedulers, Inicialización Diferida de Mundos y Cero Estáticos.
 */
@Singleton
public class QueueManager {

    private final NexoDungeons plugin;
    private final WaveManager waveManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    private final ConcurrentLinkedQueue<UUID> waveQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, LocationData> configuredArenas;

    // 🌟 FIX ANTI-CRASH: Evita buscar mundos durante el onEnable()
    private record LocationData(String worldName, double x, double y, double z) {
        public Location toLocation() {
            var world = Bukkit.getWorld(worldName);
            if (world == null) world = Bukkit.getWorlds().get(0); // Fallback de seguridad
            return new Location(world, x, y, z);
        }
    }

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public QueueManager(NexoDungeons plugin, WaveManager waveManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.waveManager = waveManager;
        this.crossplayUtils = crossplayUtils;

        // 🌟 FIX: Inicialización segura y diferida de las coordenadas
        this.configuredArenas = Map.of(
                "Sector_Coliseo", new LocationData("world", 1000, 64, 1000),
                "Sector_Infernal", new LocationData("world", -1000, 64, -1000)
        );

        iniciarMotorDeEmparejamiento();
    }

    public void addPlayerToWaves(Player p) {
        if (waveQueue.contains(p.getUniqueId())) {
            crossplayUtils.sendMessage(p, "&#FF5555[!] Ya te encuentras en la cola de emparejamiento.");
            return;
        }
        waveQueue.add(p.getUniqueId());

        crossplayUtils.sendMessage(p, "&#55FF55[✓] <bold>EMPAREJAMIENTO:</bold> &#E6CCFFTe has unido a la cola de las mazmorras.");
        crossplayUtils.sendMessage(p, "&#E6CCFFPosición actual: &#00f5ff" + waveQueue.size());
    }

    public void removePlayer(Player p) {
        if (waveQueue.remove(p.getUniqueId())) {
            crossplayUtils.sendMessage(p, "&#FF5555[!] Has abandonado la cola de emparejamiento.");
        }
    }

    private void iniciarMotorDeEmparejamiento() {
        // 🌟 PAPER/FOLIA NATIVE: Ejecución asíncrona segura atada al ciclo global
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (waveQueue.isEmpty()) return;

            for (var entry : configuredArenas.entrySet()) {
                var arenaId = entry.getKey();

                // Si la arena está libre, armamos un escuadrón
                if (!waveManager.isArenaActive(arenaId)) {

                    var escuadron = new ArrayList<Player>();
                    // Escuadrones de hasta 3 jugadores
                    while (escuadron.size() < 3 && !waveQueue.isEmpty()) {
                        var playerId = waveQueue.poll();
                        if (playerId == null) continue;

                        var p = Bukkit.getPlayer(playerId);
                        if (p != null && p.isOnline()) {
                            escuadron.add(p);
                        }
                    }

                    if (escuadron.isEmpty()) continue;

                    // Instanciamos el Location en el momento exacto en que se necesita
                    var targetLocation = entry.getValue().toLocation();

                    // Despliegue del escuadrón
                    for (var p : escuadron) {
                        // 🌟 PAPER NATIVE: Teletransporte Asíncrono puro. Cero lagazos al cargar chunks.
                        p.teleportAsync(targetLocation).thenAccept(success -> {
                            if (success) {
                                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                                crossplayUtils.sendMessage(p, "&#555555--------------------------------");
                                crossplayUtils.sendMessage(p, "&#00f5ff⚔ <bold>ARENA ENCONTRADA:</bold> &#E6CCFFDesplegando en " + arenaId.replace("_", " ") + ".");
                                crossplayUtils.sendMessage(p, "&#E6CCFFTamaño del escuadrón: &#55FF55" + escuadron.size() + " Jugador(es)");
                                crossplayUtils.sendMessage(p, "&#555555--------------------------------");
                            }
                        });
                    }

                    // Iniciamos la arena mediante el WaveManager inyectado
                    waveManager.startArena(arenaId, targetLocation);

                    if (waveQueue.isEmpty()) break;
                }
            }
        }, 20L, 40L); // Retraso de 20 ticks, repite cada 40 ticks
    }
}
package me.nexo.dungeons.matchmaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.instances.DungeonSlimeManager;
import me.nexo.dungeons.waves.WaveManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer; // 🌟 NUEVO
import net.kyori.adventure.title.Title; // 🌟 NUEVO
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration; // 🌟 NUEVO
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏰 NexoDungeons - Motor de Emparejamiento (Arquitectura ASP & Java 25)
 * Rendimiento: Virtual Threads masivos, Instancias efímeras en RAM (Cero Grid).
 */
@Singleton
public class QueueManager {

    private final NexoDungeons plugin;
    private final WaveManager waveManager;
    private final CrossplayUtils crossplayUtils;
    private final DungeonSlimeManager dungeonSlimeManager;

    // 🚀 JAVA 25: Virtual Threads para manejar las colas sin tocar el Main Thread
    private final ExecutorService matchmakingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentLinkedQueue<UUID> matchmakingQueue = new ConcurrentLinkedQueue<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public QueueManager(NexoDungeons plugin, WaveManager waveManager, CrossplayUtils crossplayUtils, DungeonSlimeManager dungeonSlimeManager) {
        this.plugin = plugin;
        this.waveManager = waveManager;
        this.crossplayUtils = crossplayUtils;
        this.dungeonSlimeManager = dungeonSlimeManager;

        // Arrancamos el procesador multihilo al inyectar la clase
        startMatchmakingProcessor();
    }

    public void addPlayerToQueue(Player p) {
        if (matchmakingQueue.contains(p.getUniqueId())) {
            crossplayUtils.sendMessage(p, "&#FF5555[!] Ya te encuentras en la cola de emparejamiento.");
            return;
        }
        matchmakingQueue.add(p.getUniqueId());

        crossplayUtils.sendMessage(p, "&#55FF55[✓] <bold>EMPAREJAMIENTO:</bold> &#E6CCFFBuscando grupo para la mazmorra...");
        crossplayUtils.sendMessage(p, "&#E6CCFFPosición actual: &#00f5ff" + matchmakingQueue.size());
    }

    public void removePlayer(Player p) {
        if (matchmakingQueue.remove(p.getUniqueId())) {
            crossplayUtils.sendMessage(p, "&#FF5555[!] Has abandonado la cola de emparejamiento.");
        }
    }

    /**
     * Bucle infinito y ultraligero montado sobre un Virtual Thread.
     */
    private void startMatchmakingProcessor() {
        matchmakingExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    processQueues();
                    Thread.sleep(1000); // Evalúa cada segundo sin afectar el TPS del servidor
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    /**
     * Lógica de agrupación (Aquí se puede expandir para comparar Gear Score)
     */
    private void processQueues() {
        if (matchmakingQueue.isEmpty()) return;

        List<Player> escuadron = new ArrayList<>();

        // Formamos escuadrones de hasta 3 jugadores
        while (escuadron.size() < 3 && !matchmakingQueue.isEmpty()) {
            UUID playerId = matchmakingQueue.poll();
            if (playerId == null) continue;

            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                escuadron.add(p);
            }
        }

        if (!escuadron.isEmpty()) {
            // Asignamos a la plantilla por defecto (puede ser dinámica después)
            createMatch(escuadron, "dungeon_template");
        }
    }

    /**
     * Conecta el emparejamiento con el motor de creación de mundos Slime.
     */
    private void createMatch(List<Player> party, String templateId) {
        UUID partyId = UUID.randomUUID();

        // 🌟 ASP API: El SlimeManager crea el mundo asíncronamente y nos devuelve el World listo
        dungeonSlimeManager.createDungeonInstance(partyId, templateId).thenAccept(world -> {
            if (world != null) {
                Location spawnLocation = new Location(world, 0, 64, 0); // Spawn universal de la plantilla

                // Teleport y UX Premium
                for (Player p : party) {
                    crossplayUtils.sendMessage(p, "&#555555--------------------------------");
                    crossplayUtils.sendMessage(p, "&#00f5ff⚔ <bold>INSTANCIA CREADA:</bold> &#E6CCFFDesplegando en la Mazmorra.");
                    crossplayUtils.sendMessage(p, "&#E6CCFFTamaño del escuadrón: &#55FF55" + party.size() + " Jugador(es)");
                    crossplayUtils.sendMessage(p, "&#555555--------------------------------");

                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

                    // 🌟 PAPER NATIVE: Teletransporte Asíncrono puro (Cero lagazos)
                    p.teleportAsync(spawnLocation).thenAccept(success -> {
                        if (success) {
                            // 🌟 FIX: Aplicamos titles modernos con Kyori Adventure
                            var mainTitle = LegacyComponentSerializer.legacyAmpersand().deserialize("&c☠ " + templateId.toUpperCase().replace("_TEMPLATE", ""));
                            var subTitle = LegacyComponentSerializer.legacyAmpersand().deserialize("&7Prepárate para la batalla");

                            // 10 ticks = 500ms, 70 ticks = 3500ms, 20 ticks = 1000ms
                            var times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000));

                            p.showTitle(Title.title(mainTitle, subTitle, times));
                        }
                    });
                }

                // Iniciamos la lógica de Oleadas/Boss en el nuevo mundo clonado
                waveManager.startArena(partyId.toString(), spawnLocation);
            }
        });
    }
}
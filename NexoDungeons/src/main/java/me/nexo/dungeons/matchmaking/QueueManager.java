package me.nexo.dungeons.matchmaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.api.IDungeonController;
import me.nexo.dungeons.engine.NexoDungeonFactory; // 🌟 NUEVA DEPENDENCIA
import me.nexo.dungeons.instances.DungeonSlimeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏰 NexoDungeons - Motor de Emparejamiento (Arquitectura ASP & Java 21+)
 * Rendimiento: Virtual Threads masivos, Instancias efímeras y Factory Pattern.
 */
@Singleton
public class QueueManager {

    private final NexoDungeons plugin;
    private final NexoDungeonFactory dungeonFactory; // 🌟 REEMPLAZO DE WAVEMANAGER
    private final CrossplayUtils crossplayUtils;
    private final DungeonSlimeManager dungeonSlimeManager;

    // 🚀 JAVA 21: Virtual Threads para manejar las colas sin tocar el Main Thread
    private final ExecutorService matchmakingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentLinkedQueue<UUID> matchmakingQueue = new ConcurrentLinkedQueue<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public QueueManager(NexoDungeons plugin, NexoDungeonFactory dungeonFactory,
                        CrossplayUtils crossplayUtils, DungeonSlimeManager dungeonSlimeManager) {
        this.plugin = plugin;
        this.dungeonFactory = dungeonFactory;
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
            // 🌟 NUEVO: Asignamos el modo PUZZLE como prueba de la nueva arquitectura
            createMatch(escuadron, "dungeon_template", "PUZZLE");
        }
    }

    /**
     * Conecta el emparejamiento con el motor de creación de mundos Slime y el Controller.
     */
    private void createMatch(List<Player> party, String templateId, String mode) {
        UUID partyId = UUID.randomUUID();

        // 🌟 ASP API: El SlimeManager crea el mundo asíncronamente
        dungeonSlimeManager.createDungeonInstance(partyId, templateId).thenAccept(world -> {
            if (world == null) return;

            // 🌟 FACTORY: Construimos el cerebro de la mazmorra (Puzzle, Wave o Summon)
            IDungeonController dungeon = dungeonFactory.createDungeon(mode, partyId, world, party);

            Location spawnLocation = new Location(world, 0, 64, 0); // Spawn universal
            List<CompletableFuture<Boolean>> teleports = new ArrayList<>();

            // 🔄 Volvemos al Main Thread temporalmente para interactuar con los jugadores de Bukkit
            Bukkit.getScheduler().runTask(plugin, () -> {

                dungeon.initialize(); // Ej: Ponerlos en modo Aventura

                // Teleport y UX Premium
                for (Player p : party) {
                    crossplayUtils.sendMessage(p, "&#555555--------------------------------");
                    crossplayUtils.sendMessage(p, "&#00f5ff⚔ <bold>INSTANCIA CREADA:</bold> &#E6CCFFDesplegando en la Mazmorra.");
                    crossplayUtils.sendMessage(p, "&#E6CCFFTamaño del escuadrón: &#55FF55" + party.size() + " Jugador(es)");
                    crossplayUtils.sendMessage(p, "&#E6CCFFModo de Operación: &#FFAA00" + mode);
                    crossplayUtils.sendMessage(p, "&#555555--------------------------------");

                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

                    // Guardamos la promesa del teleport
                    teleports.add(p.teleportAsync(spawnLocation).thenApply(success -> {
                        if (success) {
                            var mainTitle = LegacyComponentSerializer.legacyAmpersand().deserialize("&c☠ " + templateId.toUpperCase().replace("_TEMPLATE", ""));
                            var subTitle = LegacyComponentSerializer.legacyAmpersand().deserialize("&7Descendiendo a las profundidades...");
                            var times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000));
                            p.showTitle(Title.title(mainTitle, subTitle, times));
                        }
                        return success;
                    }));
                }

                // 🌟 SINCRONÍA PERFECTA: Esperamos que TODOS los jugadores se teletransporten
                CompletableFuture.allOf(teleports.toArray(new CompletableFuture[0]))
                        .thenRun(() -> {
                            // Una vez todos pisaron la arena, arranca el timer y la partida
                            Bukkit.getScheduler().runTask(plugin, dungeon::start);
                        });
            });
        });
    }
}
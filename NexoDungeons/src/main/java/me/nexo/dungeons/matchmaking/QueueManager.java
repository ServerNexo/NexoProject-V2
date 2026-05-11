package me.nexo.dungeons.matchmaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.api.IDungeonController;
import me.nexo.dungeons.engine.NexoDungeonFactory;
import me.nexo.dungeons.engine.AbyssScalingEngine; // 🌟 NUEVO IMPORT
import me.nexo.dungeons.instances.DungeonSlimeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏰 NexoDungeons - Motor de Emparejamiento Adaptativo (Arquitectura ASP & Java 21+)
 * Matchmaking con Timeout de 45s, Gear Score Scoring y Soporte Solitario Automático.
 */
@Singleton
public final class QueueManager { // 🌟 FIX 1: Añadido 'final' para seguridad del constructor

    private final NexoDungeons plugin;
    private final NexoDungeonFactory dungeonFactory;
    private final CrossplayUtils crossplayUtils;
    private final DungeonSlimeManager dungeonSlimeManager;
    private final AbyssScalingEngine scalingEngine;

    // 🚀 JAVA 21: Virtual Threads para manejar las colas sin tocar el Main Thread
    private final ExecutorService matchmakingExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Lista de tickets activos en la cola
    private final ConcurrentLinkedQueue<QueueTicket> matchmakingQueue = new ConcurrentLinkedQueue<>();

    private static final int MAX_PARTY_SIZE = 3;
    private static final long TIMEOUT_MILLIS = 45_000L; // 45 Segundos de espera máxima

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public QueueManager(NexoDungeons plugin, NexoDungeonFactory dungeonFactory,
                        CrossplayUtils crossplayUtils, DungeonSlimeManager dungeonSlimeManager,
                        AbyssScalingEngine scalingEngine) {
        this.plugin = plugin;
        this.dungeonFactory = dungeonFactory;
        this.crossplayUtils = crossplayUtils;
        this.dungeonSlimeManager = dungeonSlimeManager;
        this.scalingEngine = scalingEngine;

        startMatchmakingProcessor();
    }

    /**
     * DTO Interno para manejar el estado del jugador en la cola
     */
    private static class QueueTicket {
        UUID playerId;
        long joinTime;
        int gearScore;

        QueueTicket(UUID playerId, int gearScore) {
            this.playerId = playerId;
            this.joinTime = System.currentTimeMillis();
            this.gearScore = gearScore;
        }
    }

    /**
     * Calcula el Gear Score del jugador en tiempo real.
     */
    private int calculateGearScore(Player p) {
        return 100 + (p.getLevel() * 2);
    }

    public void addPlayerToQueue(Player p) {
        if (matchmakingQueue.stream().anyMatch(t -> t.playerId.equals(p.getUniqueId()))) {
            crossplayUtils.sendMessage(p, "&#FF5555[!] Ya te encuentras en la cola de emparejamiento.");
            return;
        }

        int gearScore = calculateGearScore(p);
        matchmakingQueue.add(new QueueTicket(p.getUniqueId(), gearScore));

        crossplayUtils.sendMessage(p, "&#55FF55[✓] <bold>EMPAREJAMIENTO:</bold> &#E6CCFFBuscando grupo para El Abismo...");
        crossplayUtils.sendMessage(p, "&#E6CCFFTiempo estimado máximo: &#00f5ff45 Segundos.");
    }

    public void removePlayer(Player p) {
        matchmakingQueue.removeIf(ticket -> ticket.playerId.equals(p.getUniqueId()));
        crossplayUtils.sendMessage(p, "&#FF5555[!] Has abandonado la cola de emparejamiento.");
    }

    /**
     * Bucle infinito y ultraligero montado sobre un Virtual Thread.
     */
    private void startMatchmakingProcessor() {
        matchmakingExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    processQueues();
                    Thread.sleep(2000); // Evalúa la cola cada 2 segundos
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    /**
     * 🌟 CORE DEL MATCHMAKING: Agrupa por Gear Score o inicia Solos por Timeout.
     */
    private void processQueues() {
        if (matchmakingQueue.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<QueueTicket> processedTickets = new ArrayList<>();

        Iterator<QueueTicket> iterator = matchmakingQueue.iterator();
        while (iterator.hasNext()) {
            QueueTicket hostTicket = iterator.next();
            if (processedTickets.contains(hostTicket)) continue;

            Player hostPlayer = Bukkit.getPlayer(hostTicket.playerId);
            if (hostPlayer == null || !hostPlayer.isOnline()) {
                iterator.remove();
                continue;
            }

            List<Player> escuadron = new ArrayList<>();
            escuadron.add(hostPlayer);
            List<QueueTicket> matchedTickets = new ArrayList<>();
            matchedTickets.add(hostTicket);

            // 1. Buscamos compañeros compatibles (Gear Score +/- 10%)
            for (QueueTicket otherTicket : matchmakingQueue) {
                if (escuadron.size() >= MAX_PARTY_SIZE) break;
                if (otherTicket == hostTicket || processedTickets.contains(otherTicket)) continue;

                Player otherPlayer = Bukkit.getPlayer(otherTicket.playerId);
                if (otherPlayer != null && otherPlayer.isOnline()) {

                    double difference = Math.abs(hostTicket.gearScore - otherTicket.gearScore) / (double) hostTicket.gearScore;
                    if (difference <= 0.10) {
                        escuadron.add(otherPlayer);
                        matchedTickets.add(otherTicket);
                    }
                }
            }

            // 2. Verificamos si podemos lanzar la partida
            boolean isTimeout = (now - hostTicket.joinTime) >= TIMEOUT_MILLIS;
            boolean isFullParty = escuadron.size() == MAX_PARTY_SIZE;

            if (isFullParty || isTimeout) {
                matchmakingQueue.removeAll(matchedTickets);
                processedTickets.addAll(matchedTickets);

                // 🌟 EJECUCIÓN: Lanzamos la instancia
                createMatch(escuadron, "dungeon_template", "PUZZLE");
            }
        }
    }

    /**
     * Conecta el emparejamiento con el motor de creación de mundos Slime y el Controller.
     */
    private void createMatch(List<Player> party, String templateId, String mode) {
        UUID partyId = UUID.randomUUID();

        dungeonSlimeManager.createDungeonInstance(partyId, templateId).thenAccept(world -> {
            if (world == null) return;

            scalingEngine.registerInstance(world.getName(), party);
            IDungeonController dungeon = dungeonFactory.createDungeon(mode, partyId, world, party);

            Location spawnLocation = new Location(world, 0, 64, 0);
            List<CompletableFuture<Boolean>> teleports = new ArrayList<>();

            Bukkit.getScheduler().runTask(plugin, () -> {

                dungeon.initialize();

                for (Player p : party) {
                    crossplayUtils.sendMessage(p, "&#555555--------------------------------");
                    crossplayUtils.sendMessage(p, "&#00f5ff⚔ <bold>INSTANCIA CREADA:</bold> &#E6CCFFDesplegando en la Mazmorra.");
                    crossplayUtils.sendMessage(p, "&#E6CCFFTamaño del escuadrón: &#55FF55" + party.size() + " Jugador(es)");
                    crossplayUtils.sendMessage(p, "&#E6CCFFModo de Operación: &#FFAA00" + mode);
                    crossplayUtils.sendMessage(p, "&#555555--------------------------------");

                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

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

                // 🌟 FIX 2: Añadido <?> al array de CompletableFuture para satisfacer los Generics de Java
                CompletableFuture.allOf(teleports.toArray(new CompletableFuture<?>[0]))
                        .thenRun(() -> {
                            Bukkit.getScheduler().runTask(plugin, dungeon::start);
                        });
            });
        });
    }
}
package me.nexo.dungeons.modes;

import com.google.inject.Inject;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.api.IDungeonController;
import me.nexo.dungeons.engine.NexoDungeonFactory;
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.instances.DungeonSlimeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 🧩 NexoDungeons - Modo Puzzle (Estilo Endgame / Hypixel)
 * Mazmorra instanciada centrada en rompecabezas, parkour y mecánicas de entorno.
 * Nota: NO lleva @Singleton porque cada partida necesita su propia instancia en RAM.
 */
public class PuzzleDungeon implements IDungeonController {

    private final NexoDungeons plugin;
    private final DungeonSlimeManager slimeManager;
    private final PuzzleEngine puzzleEngine;
    private final CrossplayUtils crossplayUtils;
    private final NexoDungeonFactory dungeonFactory;

    // 🌐 Estado Efímero de la Instancia
    private UUID instanceId;
    private World slimeWorld;
    private List<Player> party;
    private List<Player> alivePlayers;
    private boolean isRunning = false;

    // 💉 Guice inyecta los servicios pesados automáticamente
    @Inject
    public PuzzleDungeon(NexoDungeons plugin, DungeonSlimeManager slimeManager,
                         PuzzleEngine puzzleEngine, CrossplayUtils crossplayUtils,
                         NexoDungeonFactory dungeonFactory) {
        this.plugin = plugin;
        this.slimeManager = slimeManager;
        this.puzzleEngine = puzzleEngine;
        this.crossplayUtils = crossplayUtils;
        this.dungeonFactory = dungeonFactory;
    }

    // 🌟 Método de Inyección Dinámica (Llamado por nuestra Factory)
    public void setup(UUID instanceId, World slimeWorld, List<Player> party) {
        this.instanceId = instanceId;
        this.slimeWorld = slimeWorld;
        this.party = new ArrayList<>(party);
        this.alivePlayers = new ArrayList<>(party); // Al inicio, todos están vivos
    }

    @Override
    public UUID getInstanceId() { return instanceId; }

    @Override
    public World getSlimeWorld() { return slimeWorld; }

    @Override
    public void initialize() {
        for (Player p : party) {
            p.setGameMode(GameMode.ADVENTURE); // Evita que rompan la estructura de la dungeon
            crossplayUtils.sendMessage(p, "&#FFAA00[!] Sincronizando mecanismos de la mazmorra...");
        }
    }

    @Override
    public void start() {
        this.isRunning = true;

        var mainTitle = LegacyComponentSerializer.legacyAmpersand().deserialize("&6&l¡MAZMORRA INICIADA!");
        var subTitle = LegacyComponentSerializer.legacyAmpersand().deserialize("&eResuelve los acertijos para avanzar");
        var times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
        var title = Title.title(mainTitle, subTitle, times);

        for (Player p : party) {
            p.showTitle(title);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        // ⏱️ TIMER ASÍNCRONO NATIVO (Paper 1.21): 30 minutos de límite
        Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
            if (isRunning) {
                endDungeon(false); // Se acabó el tiempo
            }
        }, 30, TimeUnit.MINUTES);
    }

    // =========================================
    // 🌟 ENRUTAMIENTO DE EVENTOS (Contrato IDungeonController)
    // =========================================

    @Override
    public void handleInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        // En el modo puzzle, las interacciones suelen ser botones o palancas manejadas por PuzzleEngine
        // Si necesitas algo específico instanciado, ponlo aquí.
    }

    @Override
    public void handleMobDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        // En el modo puzzle, si mueren mobs (ej: guardianes de una sala), puedes manejar llaves aquí.
    }

    // =========================================

    @Override
    public void handlePlayerDeath(Player player) {
        if (!isRunning) return;

        alivePlayers.remove(player);

        // 👻 Revive como fantasma para ver a sus compañeros
        Bukkit.getRegionScheduler().run(plugin, player.getLocation(), task -> {
            player.spigot().respawn();
            player.setGameMode(GameMode.SPECTATOR);
        });

        crossplayUtils.sendMessage(player, "&#FF5555[☠] Has caído. Espera a que tu equipo termine.");

        for (Player p : alivePlayers) {
            crossplayUtils.sendMessage(p, "&#FF5555[!] " + player.getName() + " ha muerto. Jugadores vivos: " + alivePlayers.size());
        }

        // 💀 Si todos mueren, la mazmorra fracasa instantáneamente
        if (alivePlayers.isEmpty()) {
            endDungeon(false);
        }
    }

    @Override
    public void handlePlayerQuit(Player player) {
        // En una dungeon de puzzles hardcore, desconectarse cuenta como muerte
        handlePlayerDeath(player);
    }

    @Override
    public CompletableFuture<Void> endDungeon(boolean success) {
        this.isRunning = false;

        // 🚀 Ejecutamos el cierre en un Virtual Thread para no dar tirones de lag
        return CompletableFuture.runAsync(() -> {
            if (success) {
                // 🏆 LÓGICA DE VICTORIA
                for (Player p : party) {
                    crossplayUtils.sendMessage(p, "&#55FF55✨ ¡MAZMORRA COMPLETADA!");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
            } else {
                // ☠️ LÓGICA DE DERROTA
                var mainTitle = LegacyComponentSerializer.legacyAmpersand().deserialize("&c&l¡EQUIPO ANIQUILADO!");
                var subTitle = LegacyComponentSerializer.legacyAmpersand().deserialize("&7La mazmorra ha colapsado");
                var times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));

                for (Player p : party) {
                    if (p.isOnline()) {
                        p.showTitle(Title.title(mainTitle, subTitle, times));
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);
                    }
                }
            }

            // ⏳ Esperamos 10 segundos para que vean su loot o la pantalla de derrota
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {}

            // Volvemos al Hilo Principal solo para teletransportar
            Bukkit.getScheduler().runTask(plugin, this::destroyInstance);
        });
    }

    @Override
    public void destroyInstance() {
        Location hubLocation = Bukkit.getWorlds().get(0).getSpawnLocation(); // Reemplazar por tu Hub

        for (Player p : party) {
            if (p.isOnline()) {
                // Extracción asíncrona fluida
                p.teleportAsync(hubLocation).thenAccept(tp -> {
                    if (tp) p.setGameMode(GameMode.SURVIVAL);
                });
            }
        }

        // 🧹 LIMPIEZA DE MAPA DE RUTEO: Quitamos esta dungeon de las "Activas"
        dungeonFactory.removeActiveDungeon(slimeWorld);

        // 💥 ANHILACIÓN DE LA RAM: Le pedimos al Manager que borre el mundo Slime
        slimeManager.destroyDungeonInstance(instanceId);
    }
}
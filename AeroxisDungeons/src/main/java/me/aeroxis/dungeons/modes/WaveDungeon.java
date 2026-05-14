package me.aeroxis.dungeons.modes;

import com.google.inject.Inject;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.dungeons.AeroxisDungeons;
import me.aeroxis.dungeons.api.IDungeonController;
import me.aeroxis.dungeons.engine.AeroxisDungeonFactory;
import me.aeroxis.dungeons.instances.DungeonSlimeManager;
import me.aeroxis.dungeons.waves.WaveManager; // 🌟 NUEVO IMPORT
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

/**
 * 🌊 AeroxisDungeons - Modo Oleadas (Supervivencia / Defensa)
 * Los jugadores deben resistir X cantidad de oleadas o un tiempo determinado.
 * NO lleva @Singleton porque cada arena instanciada requiere el suyo.
 */
public class WaveDungeon implements IDungeonController {

    private final AeroxisDungeons plugin;
    private final DungeonSlimeManager slimeManager;
    private final CrossplayUtils crossplayUtils;
    private final AeroxisDungeonFactory dungeonFactory;
    private final WaveManager waveManager; // 🌟 MOTOR DE OLEADAS INYECTADO

    private UUID instanceId;
    private World slimeWorld;
    private List<Player> party;
    private List<Player> alivePlayers;
    private boolean isRunning = false;
    private int currentWave = 0;

    @Inject
    public WaveDungeon(AeroxisDungeons plugin, DungeonSlimeManager slimeManager,
                       CrossplayUtils crossplayUtils, AeroxisDungeonFactory dungeonFactory,
                       WaveManager waveManager) { // 🌟 AÑADIDO AL CONSTRUCTOR
        this.plugin = plugin;
        this.slimeManager = slimeManager;
        this.crossplayUtils = crossplayUtils;
        this.dungeonFactory = dungeonFactory;
        this.waveManager = waveManager;
    }

    public void setup(UUID instanceId, World slimeWorld, List<Player> party) {
        this.instanceId = instanceId;
        this.slimeWorld = slimeWorld;
        this.party = new ArrayList<>(party);
        this.alivePlayers = new ArrayList<>(party);
    }

    @Override public UUID getInstanceId() { return instanceId; }
    @Override public World getSlimeWorld() { return slimeWorld; }

    @Override
    public void initialize() {
        for (Player p : party) {
            p.setGameMode(GameMode.ADVENTURE);
            crossplayUtils.sendMessage(p, "&#FFAA00[!] Preparando el coliseo...");
        }
    }

    @Override
    public void start() {
        this.isRunning = true;
        var title = Title.title(
                LegacyComponentSerializer.legacyAmpersand().deserialize("&4&lMODO SUPERVIVENCIA"),
                LegacyComponentSerializer.legacyAmpersand().deserialize("&cPrepárate para la primera oleada"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
        );

        for (Player p : party) {
            p.showTitle(title);
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
        }

        // 🌟 CONECTADO: Le damos la orden al Motor de Oleadas para que empiece a escupir monstruos
        // Ajusta las coordenadas del center (0, 64, 0) si el centro de tu arena está en otro lado.
        Location center = new Location(slimeWorld, 0, 64, 0);
        waveManager.startArena(instanceId.toString(), center);
    }

    // =========================================
    // 🌟 ENRUTAMIENTO DE EVENTOS (Contrato IDungeonController)
    // =========================================

    @Override
    public void handleInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        // En WaveDungeon no usamos interacciones de bloques especiales por ahora
    }

    @Override
    public void handleMobDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        // El WaveManager ya escucha las muertes por su cuenta en su Listener O(1),
        // así que lo dejamos vacío aquí.
    }

    // =========================================

    @Override
    public void handlePlayerDeath(Player player) {
        if (!isRunning) return;
        alivePlayers.remove(player);

        Bukkit.getRegionScheduler().run(plugin, player.getLocation(), task -> {
            player.spigot().respawn();
            player.setGameMode(GameMode.SPECTATOR);
        });

        crossplayUtils.sendMessage(player, "&#FF5555[☠] Has caído en la arena.");
        for (Player p : alivePlayers) crossplayUtils.sendMessage(p, "&#FF5555[!] " + player.getName() + " ha muerto.");

        if (alivePlayers.isEmpty()) endDungeon(false);
    }

    @Override
    public void handlePlayerQuit(Player player) {
        handlePlayerDeath(player);
    }

    @Override
    public CompletableFuture<Void> endDungeon(boolean success) {
        this.isRunning = false;
        return CompletableFuture.runAsync(() -> {
            if (success) {
                for (Player p : party) {
                    crossplayUtils.sendMessage(p, "&#55FF55✨ ¡HAS SOBREVIVIDO A TODAS LAS OLEADAS!");
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
            } else {
                for (Player p : party) {
                    if (p.isOnline()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);
                }
            }

            try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
            Bukkit.getScheduler().runTask(plugin, this::destroyInstance);
        });
    }

    @Override
    public void destroyInstance() {
        Location hub = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player p : party) {
            if (p.isOnline()) {
                p.teleportAsync(hub).thenAccept(tp -> { if (tp) p.setGameMode(GameMode.SURVIVAL); });
            }
        }

        // 🌟 CONECTADO: Apagamos el motor de monstruos para evitar leaks
        waveManager.stopArena(instanceId.toString());

        // Limpiamos el enrutador y destruimos el mundo
        dungeonFactory.removeActiveDungeon(slimeWorld);
        slimeManager.destroyDungeonInstance(instanceId);
    }
}
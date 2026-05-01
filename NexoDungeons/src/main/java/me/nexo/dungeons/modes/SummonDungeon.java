package me.nexo.dungeons.modes;

import com.google.inject.Inject;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.api.IDungeonController;
import me.nexo.dungeons.engine.NexoDungeonFactory;
import me.nexo.dungeons.instances.DungeonSlimeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity; // 🌟 NUEVO IMPORT
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 🔮 NexoDungeons - Modo Invocación (Grinding & Ritual)
 * Los jugadores farmean monstruos menores para conseguir fragmentos y despertar al Jefe.
 */
public class SummonDungeon implements IDungeonController {

    private final NexoDungeons plugin;
    private final DungeonSlimeManager slimeManager;
    private final CrossplayUtils crossplayUtils;
    private final NexoDungeonFactory dungeonFactory;

    private UUID instanceId;
    private World slimeWorld;
    private List<Player> party;
    private List<Player> alivePlayers;

    private boolean isRunning = false;
    private boolean bossSpawned = false;

    // 🌟 VARIABLES DEL RITUAL
    private int fragmentosDepositados = 0;
    private final int FRAGMENTOS_NECESARIOS = 15;
    private Location altarLocation;

    @Inject
    public SummonDungeon(NexoDungeons plugin, DungeonSlimeManager slimeManager,
                         CrossplayUtils crossplayUtils, NexoDungeonFactory dungeonFactory) {
        this.plugin = plugin;
        this.slimeManager = slimeManager;
        this.crossplayUtils = crossplayUtils;
        this.dungeonFactory = dungeonFactory;
    }

    public void setup(UUID instanceId, World slimeWorld, List<Player> party) {
        this.instanceId = instanceId;
        this.slimeWorld = slimeWorld;
        this.party = new ArrayList<>(party);
        this.alivePlayers = new ArrayList<>(party);

        // Asumimos que el Altar de Invocación está en el centro del mapa
        this.altarLocation = new Location(slimeWorld, 0, 64, 0);
    }

    @Override public UUID getInstanceId() { return instanceId; }
    @Override public World getSlimeWorld() { return slimeWorld; }

    @Override
    public void initialize() {
        for (Player p : party) {
            p.setGameMode(GameMode.SURVIVAL);
            crossplayUtils.sendMessage(p, "&#FFAA00[!] El altar (Lodestone) está inactivo. Requiere " + FRAGMENTOS_NECESARIOS + " almas.");
        }
    }

    @Override
    public void start() {
        this.isRunning = true;
        var title = Title.title(
                LegacyComponentSerializer.legacyAmpersand().deserialize("&5&lRITUAL DE SANGRE"),
                LegacyComponentSerializer.legacyAmpersand().deserialize("&dDeposita fragmentos de alma en el Altar"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
        );

        for (Player p : party) {
            p.showTitle(title);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.8f);
        }

        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            if (!isRunning || bossSpawned) {
                task.cancel();
                return;
            }
            if (altarLocation.getWorld() != null) {
                altarLocation.getWorld().spawnParticle(Particle.PORTAL, altarLocation.clone().add(0.5, 1, 0.5), 10, 0.2, 0.5, 0.2, 0.05);
            }
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void handleInteract(PlayerInteractEvent event) {
        if (!isRunning || bossSpawned) return;

        var block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LODESTONE) return;

        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();

        if (hand.getType() == Material.AMETHYST_SHARD && hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
            event.setCancelled(true);

            hand.setAmount(hand.getAmount() - 1);
            fragmentosDepositados++;

            altarLocation.getWorld().playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.5f);

            for (Player player : party) {
                crossplayUtils.sendMessage(player, "&#E6CCFF✨ Almas en el Altar: &#55FF55" + fragmentosDepositados + "/" + FRAGMENTOS_NECESARIOS);
            }

            if (fragmentosDepositados >= FRAGMENTOS_NECESARIOS) {
                spawnBoss();
            }
        }
    }

    private void spawnBoss() {
        this.bossSpawned = true;
        Location spawnLoc = altarLocation.clone().add(0.5, 2, 0.5);

        for (Player p : party) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
            crossplayUtils.sendMessage(p, "&#FF5555💀 <bold>¡EL RENACIDO HA DESPERTADO!</bold>");
        }

        // 🌟 FIX: SPAWNEO NATIVO (Adiós errores de dependencias cruzadas con NexoCore)
        Bukkit.getRegionScheduler().run(plugin, spawnLoc, task -> {
            if (spawnLoc.getWorld() != null) {
                spawnLoc.getWorld().spawnParticle(Particle.EXPLOSION, spawnLoc, 5); // 🌟 Partícula segura 1.21

                LivingEntity boss = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
                boss.customName(LegacyComponentSerializer.legacyAmpersand().deserialize("&4&lEl Renacido"));
                boss.setCustomNameVisible(true);

                // Si quieres buffearle la vida al instante:
                // var hp = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                // if (hp != null) { hp.setBaseValue(1000.0); boss.setHealth(1000.0); }
            }
        });
    }

    @Override
    public void handleMobDeath(EntityDeathEvent event) {
        if (!isRunning || bossSpawned) return;

        if (event.getEntityType() == EntityType.ZOMBIE || event.getEntityType() == EntityType.SKELETON) {
            if (Math.random() <= 0.30) {
                ItemStack shard = new ItemStack(Material.AMETHYST_SHARD);
                var meta = shard.getItemMeta();
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&d✨ Fragmento de Alma"));
                shard.setItemMeta(meta);

                event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), shard);
            }
        }

        if (bossSpawned && event.getEntityType() == EntityType.WITHER_SKELETON) {
            endDungeon(true);
        }
    }

    @Override
    public void handlePlayerDeath(Player player) {
        if (!isRunning) return;
        alivePlayers.remove(player);
        Bukkit.getRegionScheduler().run(plugin, player.getLocation(), task -> {
            player.spigot().respawn();
            player.setGameMode(GameMode.SPECTATOR);
        });
        if (alivePlayers.isEmpty()) endDungeon(false);
    }
    @Override public void handlePlayerQuit(Player player) { handlePlayerDeath(player); }

    @Override
    public CompletableFuture<Void> endDungeon(boolean success) {
        this.isRunning = false;
        return CompletableFuture.runAsync(() -> {
            if (success) {
                for (Player p : party) crossplayUtils.sendMessage(p, "&#55FF55✨ ¡EL JEFE FUE DERROTADO!");
                slimeManager.spawnBossLoot(instanceId, altarLocation);
            }
            try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
            Bukkit.getScheduler().runTask(plugin, this::destroyInstance);
        });
    }

    @Override
    public void destroyInstance() {
        Location hub = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player p : party) {
            if (p.isOnline()) { p.teleportAsync(hub).thenAccept(tp -> { if (tp) p.setGameMode(GameMode.SURVIVAL); }); }
        }
        dungeonFactory.removeActiveDungeon(slimeWorld);
        slimeManager.destroyDungeonInstance(instanceId);
    }
}
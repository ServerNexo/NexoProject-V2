package me.nexo.dungeons.modes;

import com.google.inject.Inject;
import com.nexomc.nexo.api.NexoItems;
import me.nexo.core.bosses.NexoBossRegistry;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.api.IDungeonController;
import me.nexo.dungeons.engine.NexoDungeonFactory;
import me.nexo.dungeons.instances.DungeonSlimeManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 🔮 NexoDungeons - Modo Invocación (Zealot Ritual)
 * Arquitectura Folia-Ready | Animación Cinemática AAA | Boss API
 */
public class SummonDungeon implements IDungeonController {

    private final NexoDungeons plugin;
    private final DungeonSlimeManager slimeManager;
    private final CrossplayUtils crossplayUtils;
    private final NexoDungeonFactory dungeonFactory;
    private final NexoBossRegistry bossRegistry;

    private final NamespacedKey portadorKey;
    private final NamespacedKey bossKey;

    private UUID instanceId;
    private World slimeWorld;
    private List<Player> party;
    private List<Player> alivePlayers;

    private boolean isRunning = false;
    private boolean bossSpawned = false;

    private record FragmentoDepositado(UUID owner, ItemDisplay display) {}
    private final List<FragmentoDepositado> fragmentosActivos = new ArrayList<>();

    private final int FRAGMENTOS_NECESARIOS = 15;
    private Location altarLocation;

    @Inject
    public SummonDungeon(NexoDungeons plugin, DungeonSlimeManager slimeManager,
                         CrossplayUtils crossplayUtils, NexoDungeonFactory dungeonFactory,
                         NexoBossRegistry bossRegistry) {
        this.plugin = plugin;
        this.slimeManager = slimeManager;
        this.crossplayUtils = crossplayUtils;
        this.dungeonFactory = dungeonFactory;
        this.bossRegistry = bossRegistry;

        this.portadorKey = new NamespacedKey(plugin, "nexo_portador");
        this.bossKey = new NamespacedKey(plugin.getServer().getPluginManager().getPlugin("NexoCore"), "boss_id");
    }

    public void setup(UUID instanceId, World slimeWorld, List<Player> party, Location altarLoc) {
        this.instanceId = instanceId;
        this.slimeWorld = slimeWorld;
        this.party = new ArrayList<>(party);
        this.alivePlayers = new ArrayList<>(party);

        // 🌟 FIX CENTRADO MAGISTRAL: Convertimos a bloque exacto y centramos a fuego
        this.altarLocation = altarLoc.getBlock().getLocation().add(0.5, 0, 0.5);

        this.isRunning = false;
        this.bossSpawned = false;

        for (FragmentoDepositado fd : fragmentosActivos) {
            if (fd.display() != null) fd.display().remove();
        }
        this.fragmentosActivos.clear();
    }

    @Override public UUID getInstanceId() { return instanceId; }
    @Override public World getSlimeWorld() { return slimeWorld; }

    private void enviarMensaje(Player p, String mensajeHex) {
        String parseado = mensajeHex.replaceAll("&#([0-9a-fA-F]{6})", "<#$1>");
        p.sendMessage(MiniMessage.miniMessage().deserialize(parseado));
    }

    @Override
    public void initialize() {
        for (Player p : party) {
            p.setGameMode(GameMode.SURVIVAL);
            enviarMensaje(p, "&#FFAA00[!] El altar requiere " + FRAGMENTOS_NECESARIOS + " almas para el ritual.");
        }
    }

    @Override
    public void start() {
        this.isRunning = true;
        var title = Title.title(
                LegacyComponentSerializer.legacyAmpersand().deserialize("&5&lRITUAL DE SANGRE"),
                LegacyComponentSerializer.legacyAmpersand().deserialize("&dCaza a los Portadores de Alma y deposítalos"),
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
                altarLocation.getWorld().spawnParticle(Particle.PORTAL, altarLocation.clone().add(0, 1, 0), 10, 0.2, 0.5, 0.2, 0.05);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void handleInteract(PlayerInteractEvent event) {
        if (!isRunning || bossSpawned) return;

        // 🌟 FIX DEL "2 EN 2": Bloqueamos el evento si proviene de la mano secundaria (Off-Hand)
        if (event.getHand() != EquipmentSlot.HAND) return;

        var block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LODESTONE) return;
        event.setCancelled(true);

        Player p = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (fragmentosActivos.size() >= FRAGMENTOS_NECESARIOS) return;

            ItemStack hand = p.getInventory().getItemInMainHand();
            String nexoId = NexoItems.idFromItem(hand);

            if (nexoId != null && nexoId.equals("fragmento_alma")) {
                hand.setAmount(hand.getAmount() - 1);

                Location center = altarLocation.clone().add(0, 1.2, 0);
                Bukkit.getRegionScheduler().run(plugin, center, task -> {
                    ItemDisplay display = (ItemDisplay) center.getWorld().spawnEntity(center, EntityType.ITEM_DISPLAY);

                    var builder = NexoItems.itemFromId("fragmento_alma");
                    if (builder != null) display.setItemStack(builder.build());

                    display.setBillboard(Display.Billboard.FIXED);

                    fragmentosActivos.add(new FragmentoDepositado(p.getUniqueId(), display));
                    altarLocation.getWorld().playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 1.5f);

                    for (Player partyMember : party) {
                        enviarMensaje(partyMember, "&#E6CCFF✨ Almas en el Altar: &#55FF55" + fragmentosActivos.size() + "/" + FRAGMENTOS_NECESARIOS);
                    }

                    actualizarOrbitas();

                    if (fragmentosActivos.size() >= FRAGMENTOS_NECESARIOS) {
                        iniciarRitualCinematico();
                    }
                });
            }
        }
        else if (action == Action.LEFT_CLICK_BLOCK) {
            for (int i = fragmentosActivos.size() - 1; i >= 0; i--) {
                FragmentoDepositado fd = fragmentosActivos.get(i);
                if (fd.owner().equals(p.getUniqueId())) {
                    fd.display().remove();
                    fragmentosActivos.remove(i);

                    var builder = NexoItems.itemFromId("fragmento_alma");
                    if (builder != null) p.getInventory().addItem(builder.build());

                    altarLocation.getWorld().playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 1.5f);

                    for (Player partyMember : party) {
                        enviarMensaje(partyMember, "&#FF5555⚠️ Un alma fue retirada: &#55FF55" + fragmentosActivos.size() + "/" + FRAGMENTOS_NECESARIOS);
                    }

                    actualizarOrbitas();
                    break;
                }
            }
        }
    }

    private void actualizarOrbitas() {
        int total = fragmentosActivos.size();
        if (total == 0) return;

        double radius = 1.2;
        for (int i = 0; i < total; i++) {
            FragmentoDepositado fd = fragmentosActivos.get(i);

            double angle = 2 * Math.PI * i / total;
            float x = (float) (radius * Math.cos(angle));
            float z = (float) (radius * Math.sin(angle));

            fd.display().setInterpolationDuration(10);
            fd.display().setInterpolationDelay(0);

            Quaternionf rotacionIzquierda = new Quaternionf().rotationY((float) -angle + (float) Math.PI / 2);
            Quaternionf rotacionDerecha = new Quaternionf().rotationX((float) Math.PI / 2.5f);

            Transformation transform = new Transformation(
                    new Vector3f(x, 0f, z),
                    rotacionIzquierda,
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    rotacionDerecha
            );
            fd.display().setTransformation(transform);
        }
    }

    // ==========================================
    // 🎬 ANIMACIÓN CINEMÁTICA AAA
    // ==========================================
    private void iniciarRitualCinematico() {
        this.bossSpawned = true;

        for (Player p : party) {
            enviarMensaje(p, "&#FF5555💀 <bold>¡LA SANGRE SE CONCENTRA!</bold>");
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 0.5f);
        }

        // 🌟 MOTOR DE ANIMACIÓN POR TICKS
        final int[] tick = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            tick[0]++;
            int t = tick[0];
            Location center = altarLocation.clone().add(0, 1.2, 0);

            // FASE 1: Aceleración y Elevación (0 a 40 ticks)
            if (t <= 40) {
                double currentRadius = 1.2 * (1.0 - (t / 50.0)); // Se cierra hacia el centro
                double currentY = 1.5 * (t / 40.0); // Se elevan
                double speedMult = 1.0 + (t / 5.0); // Giran cada vez más rápido

                for (int i = 0; i < fragmentosActivos.size(); i++) {
                    FragmentoDepositado fd = fragmentosActivos.get(i);
                    double angle = (2 * Math.PI * i / fragmentosActivos.size()) + (t * 0.2 * speedMult);

                    float x = (float) (currentRadius * Math.cos(angle));
                    float z = (float) (currentRadius * Math.sin(angle));

                    Transformation transform = new Transformation(
                            new Vector3f(x, (float) currentY, z),
                            new Quaternionf().rotationY((float) -angle + (float) Math.PI / 2),
                            new Vector3f(0.5f, 0.5f, 0.5f),
                            new Quaternionf().rotationX((float) Math.PI / 2.5f)
                    );

                    fd.display().setInterpolationDuration(2); // Updates rápidos
                    fd.display().setTransformation(transform);
                }

                center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(0, currentY, 0), 10, 0.5, 0.5, 0.5, 0.2);
            }
            // FASE 2: Tensión máxima (40 a 60 ticks)
            else if (t < 60) {
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1.5, 0), 20, 0.2, 0.2, 0.2, 0.1);
            }
            // FASE 3: Explosión e Invocación
            else {
                center.getWorld().strikeLightningEffect(center);
                center.getWorld().spawnParticle(Particle.EXPLOSION, center.clone().add(0, 1, 0), 3);

                for (FragmentoDepositado fd : fragmentosActivos) {
                    fd.display().remove();
                }
                fragmentosActivos.clear();

                spawnBoss();
                task.cancel();
            }
        }, 0L, 1L); // Ejecutar cada 1 Tick (50ms)
    }

    private void spawnBoss() {
        Location spawnLoc = altarLocation.clone().add(0, 2, 0);

        for (Player p : party) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
            enviarMensaje(p, "&#FF5555💀 <bold>¡EL RENACIDO HA DESPERTADO!</bold>");
        }

        Bukkit.getRegionScheduler().run(plugin, spawnLoc, task -> {
            if (spawnLoc.getWorld() != null) {
                bossRegistry.spawnBoss("EL_RENACIDO", plugin, spawnLoc, 100);
            }
        });
    }

    @Override
    public void handleMobDeath(EntityDeathEvent event) {
        if (!isRunning || bossSpawned) return;

        LivingEntity deadEntity = event.getEntity();
        Location deathLoc = deadEntity.getLocation();

        if (bossSpawned && deadEntity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING)) {
            endDungeon(true);
            return;
        }

        if (deadEntity.getPersistentDataContainer().has(portadorKey, PersistentDataType.BYTE)) {
            var builder = NexoItems.itemFromId("fragmento_alma");
            if (builder != null) {
                deathLoc.getWorld().dropItemNaturally(deathLoc, builder.build());
            }
            deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
            return;
        }

        if (event.getEntityType() == EntityType.ZOMBIE || event.getEntityType() == EntityType.SKELETON) {
            if (Math.random() <= 0.10) {
                Bukkit.getRegionScheduler().run(plugin, deathLoc, task -> {
                    LivingEntity portador = (LivingEntity) deathLoc.getWorld().spawnEntity(deathLoc, EntityType.ENDERMAN);
                    portador.customName(LegacyComponentSerializer.legacyAmpersand().deserialize("&d&lPortador de Alma"));
                    portador.setCustomNameVisible(true);
                    portador.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 2, false, false));

                    portador.getPersistentDataContainer().set(portadorKey, PersistentDataType.BYTE, (byte) 1);

                    deathLoc.getWorld().spawnParticle(Particle.PORTAL, deathLoc, 30, 0.5, 1, 0.5, 0.1);
                    deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                });
            }
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
                for (Player p : party) enviarMensaje(p, "&#55FF55✨ ¡EL JEFE FUE DERROTADO!");
                slimeManager.spawnBossLoot(instanceId, altarLocation);
            }

            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> destroyInstance(), 10, TimeUnit.SECONDS);
        });
    }

    @Override
    public void destroyInstance() {
        Location hub = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player p : party) {
            if (p.isOnline()) { p.teleportAsync(hub).thenAccept(tp -> { if (tp) p.setGameMode(GameMode.SURVIVAL); }); }
        }

        for (FragmentoDepositado fd : fragmentosActivos) {
            if (fd.display() != null) fd.display().remove();
        }
        fragmentosActivos.clear();

        dungeonFactory.removeActiveDungeon(slimeWorld);
        slimeManager.destroyDungeonInstance(instanceId);
    }
}
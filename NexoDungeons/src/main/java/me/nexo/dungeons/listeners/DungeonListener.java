package me.nexo.dungeons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.api.IDungeonController;
import me.nexo.dungeons.data.EventRule;
import me.nexo.dungeons.engine.NexoDungeonFactory; // 🌟 NUEVA DEPENDENCIA
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.waves.WaveManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent; // 🌟 NUEVO IMPORT PARA MOBS
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Listener y Motor de Puzzles (Arquitectura Enterprise Nativa)
 * Rendimiento: Virtual Threads Puros, Schedulers Folia-Ready y Enrutamiento de Instancias.
 */
@Singleton
public class DungeonListener implements Listener {

    private final NexoDungeons plugin;
    private final PuzzleEngine puzzleEngine;
    private final WaveManager waveManager;
    private final CrossplayUtils crossplayUtils;
    private final NexoDungeonFactory dungeonFactory; // 🌟 ENRUTADOR DE MAZMORRAS

    private final Map<UUID, Long> antiSpamCooldown = new ConcurrentHashMap<>();
    private final Map<String, Integer> globalCounters = new ConcurrentHashMap<>();

    @Inject
    public DungeonListener(NexoDungeons plugin, PuzzleEngine puzzleEngine,
                           WaveManager waveManager, CrossplayUtils crossplayUtils,
                           NexoDungeonFactory dungeonFactory) {
        this.plugin = plugin;
        this.puzzleEngine = puzzleEngine;
        this.waveManager = waveManager;
        this.crossplayUtils = crossplayUtils;
        this.dungeonFactory = dungeonFactory;
    }

    // =========================================
    // 💀 GESTIÓN DEL CICLO DE VIDA Y EVENTOS DE INSTANCIA
    // =========================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();

        // Consultamos si el mundo en el que murió es una mazmorra activa
        IDungeonController activeDungeon = dungeonFactory.getActiveDungeon(p.getWorld());
        if (activeDungeon != null) {
            // Cancelamos los drops y mensajes vanilla para que el Controller decida qué hacer
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();

            // Le pasamos la batuta al modo de juego correspondiente
            activeDungeon.handlePlayerDeath(p);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        antiSpamCooldown.remove(p.getUniqueId());
        globalCounters.remove(p.getUniqueId().toString());

        // Si se desconecta en medio de una mazmorra...
        IDungeonController activeDungeon = dungeonFactory.getActiveDungeon(p.getWorld());
        if (activeDungeon != null) {
            activeDungeon.handlePlayerQuit(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        // Enrutar a la mazmorra activa (Para drops de fragmentos del Modo Summon)
        IDungeonController activeDungeon = dungeonFactory.getActiveDungeon(event.getEntity().getWorld());
        if (activeDungeon != null) {
            activeDungeon.handleMobDeath(event);
        }
    }

    // =========================================
    // 🖱️ 1. INTERACTUAR Y RUTEO DEL ALTAR
    // =========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 🌟 Ruteo hacia la Dungeon Activa (Altar, botones de inicio, etc.)
        IDungeonController activeDungeon = dungeonFactory.getActiveDungeon(event.getPlayer().getWorld());
        if (activeDungeon != null) {
            activeDungeon.handleInteract(event);
        }

        // Lógica original de Puzzles
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) return;
        var block = event.getClickedBlock();
        if (block == null) return;
        procesarEventoMotor(event.getPlayer(), block, "PLAYER_INTERACT", event);
    }

    // =========================================
    // ⛏️ 2. ROMPER BLOQUES
    // =========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        procesarEventoMotor(event.getPlayer(), event.getBlock(), "BLOCK_BREAK", event);
    }

    // =========================================
    // 🧠 3. EL CEREBRO DEL MOTOR DE PUZZLES
    // =========================================
    private void procesarEventoMotor(Player p, org.bukkit.block.Block block, String triggerType, org.bukkit.event.Cancellable bukkitEvent) {
        var rule = puzzleEngine.getRuleAt(block.getLocation());
        if (rule == null) return;

        if (!rule.trigger().type().equalsIgnoreCase(triggerType)) return;

        if (!rule.trigger().material().equalsIgnoreCase("ANY") && !rule.trigger().material().equalsIgnoreCase(block.getType().name())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (antiSpamCooldown.containsKey(p.getUniqueId()) && (now - antiSpamCooldown.get(p.getUniqueId()) < 100)) {
            bukkitEvent.setCancelled(true);
            return;
        }
        antiSpamCooldown.put(p.getUniqueId(), now);
        bukkitEvent.setCancelled(true);

        Thread.startVirtualThread(() -> {
            for (var action : rule.actions()) {
                ejecutarAccion(p, action, block.getLocation(), rule);
            }
        });
    }

    // =========================================
    // 🎬 4. EJECUTOR DE ACCIONES (Nativo PaperMC)
    // =========================================
    private void ejecutarAccion(Player p, EventRule.Action action, Location baseLoc, EventRule rule) {
        try {
            switch (action.type().toUpperCase()) {

                case "PLAY_SOUND" -> {
                    float volume = action.volume() != null ? action.volume().floatValue() : 1.0f;
                    float pitch = action.pitch() != null ? action.pitch().floatValue() : 1.0f;
                    String soundKey = action.sound().toLowerCase().replace("_", ".");
                    var sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundKey));

                    if (sound != null) {
                        Bukkit.getRegionScheduler().run(plugin, baseLoc, task -> baseLoc.getWorld().playSound(baseLoc, sound, volume, pitch));
                    }
                }

                case "CONSUME_NEXO_ITEM" -> {
                    p.getScheduler().run(plugin, task -> {
                        boolean consumed = false;
                        String targetId = action.itemId();
                        int amountNeeded = action.amount() != null ? action.amount() : 1;

                        for (var item : p.getInventory().getContents()) {
                            if (item != null && !item.isEmpty() && item.getType().name().equalsIgnoreCase(targetId)) {
                                if (item.getAmount() >= amountNeeded) {
                                    item.setAmount(item.getAmount() - amountNeeded);
                                    consumed = true;
                                    break;
                                }
                            }
                        }
                        if (!consumed) {
                            crossplayUtils.sendMessage(p, "&#FF5555[!] No posees el artefacto necesario para activar este mecanismo.");
                        }
                    }, null);
                }

                case "SPAWN_BOSS" -> {
                    int offsetX = rule.isInstanced() ? (baseLoc.getBlockX() - rule.trigger().loc().get("x")) : 0;
                    int offsetZ = rule.isInstanced() ? (baseLoc.getBlockZ() - rule.trigger().loc().get("z")) : 0;

                    double x = action.loc().getOrDefault("x", baseLoc.getBlockX()) + offsetX;
                    double y = action.loc().getOrDefault("y", baseLoc.getBlockY());
                    double z = action.loc().getOrDefault("z", baseLoc.getBlockZ()) + offsetZ;
                    var spawnLoc = new Location(baseLoc.getWorld(), x, y, z);

                    Bukkit.getRegionScheduler().run(plugin, spawnLoc, task -> {
                        LivingEntity boss = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
                        String bossName = "&4&l" + (action.mobId() != null ? action.mobId() : "Nexo Boss");
                        boss.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(bossName));
                        boss.setCustomNameVisible(true);
                    });
                }

                case "REPLACE_BLOCKS" -> {
                    var loc1 = action.loc1();
                    var loc2 = action.loc2();
                    if (loc1 == null || loc2 == null) return;

                    int offsetX = 0, offsetY = 0, offsetZ = 0;
                    if (rule.isInstanced()) {
                        offsetX = baseLoc.getBlockX() - rule.trigger().loc().get("x");
                        offsetY = baseLoc.getBlockY() - rule.trigger().loc().get("y");
                        offsetZ = baseLoc.getBlockZ() - rule.trigger().loc().get("z");
                    }

                    int minX = Math.min(loc1.get("x"), loc2.get("x")) + offsetX;
                    int minY = Math.min(loc1.get("y"), loc2.get("y")) + offsetY;
                    int minZ = Math.min(loc1.get("z"), loc2.get("z")) + offsetZ;
                    int maxX = Math.max(loc1.get("x"), loc2.get("x")) + offsetX;
                    int maxY = Math.max(loc1.get("y"), loc2.get("y")) + offsetY;
                    int maxZ = Math.max(loc1.get("z"), loc2.get("z")) + offsetZ;

                    var material = Material.valueOf(action.material().toUpperCase());

                    Bukkit.getRegionScheduler().run(plugin, baseLoc, task -> {
                        for (int x = minX; x <= maxX; x++) {
                            for (int y = minY; y <= maxY; y++) {
                                for (int z = minZ; z <= maxZ; z++) {
                                    baseLoc.getWorld().getBlockAt(x, y, z).setType(material, false);
                                }
                            }
                        }
                    });
                }

                case "START_WAVE_ARENA" -> {
                    String arenaId = action.counterId();
                    Bukkit.getRegionScheduler().run(plugin, baseLoc, task -> waveManager.startArena(arenaId, baseLoc));
                }

                default -> plugin.getLogger().warning("⚠️ Acción desconocida: " + action.type());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error ejecutando acción (" + action.type() + "): " + e.getMessage());
        }
    }
}
package me.nexo.dungeons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.data.EventRule;
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.waves.WaveManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Listener y Motor de Puzzles (Arquitectura Enterprise)
 */
@Singleton
public class DungeonListener implements Listener {

    private final NexoDungeons plugin;
    private final PuzzleEngine puzzleEngine;
    private final WaveManager waveManager;

    private final Map<UUID, Long> antiSpamCooldown = new ConcurrentHashMap<>();
    private final Map<String, Integer> globalCounters = new ConcurrentHashMap<>();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public DungeonListener(NexoDungeons plugin, PuzzleEngine puzzleEngine, WaveManager waveManager) {
        this.plugin = plugin;
        this.puzzleEngine = puzzleEngine;
        this.waveManager = waveManager;
    }

    // =========================================
    // 🖱️ 1. INTERACTUAR
    // =========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
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
    // 🧠 3. EL CEREBRO DEL MOTOR
    // =========================================
    private void procesarEventoMotor(Player p, Block block, String triggerType, org.bukkit.event.Cancellable bukkitEvent) {
        EventRule rule = puzzleEngine.getRuleAt(block.getLocation());
        if (rule == null) return;

        if (!rule.trigger().type().equalsIgnoreCase(triggerType)) return;

        if (!rule.trigger().material().equalsIgnoreCase("ANY") && !rule.trigger().material().equalsIgnoreCase(block.getType().name())) {
            return;
        }

        long now = System.currentTimeMillis();
        // Evitar que el jugador spammee la palanca/bloque y bugee la mazmorra
        if (antiSpamCooldown.containsKey(p.getUniqueId()) && (now - antiSpamCooldown.get(p.getUniqueId()) < 100)) {
            bukkitEvent.setCancelled(true);
            return;
        }
        antiSpamCooldown.put(p.getUniqueId(), now);
        bukkitEvent.setCancelled(true);

        // 🚀 Java 21 Virtual Threads (Máximo rendimiento)
        Thread.startVirtualThread(() -> {
            for (EventRule.Action action : rule.actions()) {
                ejecutarAccion(p, action, block.getLocation(), rule);
            }
        });
    }

    // =========================================
    // 🎬 4. EJECUTOR DE ACCIONES
    // =========================================
    private void ejecutarAccion(Player p, EventRule.Action action, Location baseLoc, EventRule rule) {
        try {
            switch (action.type().toUpperCase()) {

                // 🔊 Reproducir Sonido
                case "PLAY_SOUND" -> {
                    float volume = action.volume() != null ? action.volume().floatValue() : 1.0f;
                    float pitch = action.pitch() != null ? action.pitch().floatValue() : 1.0f;

                    // 🌟 PAPER 1.21+: Adaptación al nuevo sistema de Registros
                    String soundKey = action.sound().toLowerCase().replace("_", ".");
                    Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(soundKey));

                    if (sound != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> baseLoc.getWorld().playSound(baseLoc, sound, volume, pitch));
                    } else {
                        plugin.getLogger().warning("⚠️ El sonido '" + action.sound() + "' no se encontró en el registro de Minecraft.");
                    }
                }

                // 📦 Consumir Ítem
                case "CONSUME_NEXO_ITEM" -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        boolean consumed = false;
                        String targetId = action.itemId();
                        int amountNeeded = action.amount() != null ? action.amount() : 1;

                        for (ItemStack item : p.getInventory().getContents()) {
                            if (item != null && item.getType().name().equalsIgnoreCase(targetId)) {
                                if (item.getAmount() >= amountNeeded) {
                                    item.setAmount(item.getAmount() - amountNeeded);
                                    consumed = true;
                                    break;
                                }
                            }
                        }
                        if (!consumed) {
                            CrossplayUtils.sendMessage(p, "&#FF5555[!] No posees el artefacto necesario para activar este mecanismo.");
                        }
                    });
                }

                // 🐉 Invocar Boss
                case "SPAWN_MYTHICMOB" -> {
                    String mobId = action.mobId();

                    // Cálculo de Offset por si es Instanciado
                    int offsetX = rule.isInstanced() ? (baseLoc.getBlockX() - rule.trigger().loc().get("x")) : 0;
                    int offsetZ = rule.isInstanced() ? (baseLoc.getBlockZ() - rule.trigger().loc().get("z")) : 0;

                    double x = action.loc().getOrDefault("x", baseLoc.getBlockX()) + offsetX;
                    double y = action.loc().getOrDefault("y", baseLoc.getBlockY());
                    double z = action.loc().getOrDefault("z", baseLoc.getBlockZ()) + offsetZ;
                    Location spawnLoc = new Location(baseLoc.getWorld(), x, y, z);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().spawnMob(mobId, spawnLoc);
                    });
                }

                // 🧱 REEMPLAZAR BLOQUES CON FAWE
                case "REPLACE_BLOCKS" -> {
                    Map<String, Integer> loc1 = action.loc1();
                    Map<String, Integer> loc2 = action.loc2();
                    if (loc1 == null || loc2 == null) return;

                    int offsetX = 0, offsetY = 0, offsetZ = 0;
                    if (rule.isInstanced()) {
                        offsetX = baseLoc.getBlockX() - rule.trigger().loc().get("x");
                        offsetY = baseLoc.getBlockY() - rule.trigger().loc().get("y");
                        offsetZ = baseLoc.getBlockZ() - rule.trigger().loc().get("z");
                    }

                    int x1 = loc1.get("x") + offsetX;
                    int y1 = loc1.get("y") + offsetY;
                    int z1 = loc1.get("z") + offsetZ;

                    int x2 = loc2.get("x") + offsetX;
                    int y2 = loc2.get("y") + offsetY;
                    int z2 = loc2.get("z") + offsetZ;

                    Material material = Material.valueOf(action.material().toUpperCase());
                    com.sk89q.worldedit.world.block.BlockState weBlock = BukkitAdapter.adapt(material.createBlockData());
                    com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(baseLoc.getWorld());

                    // FAWE EditSession: Edición de cuboides sin lag
                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                        CuboidRegion region = new CuboidRegion(weWorld, BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));
                        editSession.setBlocks(region, weBlock);
                    }
                }

                // ⚔️ Iniciar Oleadas
                case "START_WAVE_ARENA" -> {
                    String arenaId = action.counterId();
                    Bukkit.getScheduler().runTask(plugin, () -> waveManager.startArena(arenaId, baseLoc));
                }

                default -> plugin.getLogger().warning("⚠️ Acción desconocida en el PuzzleEngine: " + action.type());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico ejecutando acción (" + action.type() + "): " + e.getMessage());
        }
    }

    // ==========================================
    // 🧹 PREVENCIÓN DE FUGAS DE MEMORIA (RAM)
    // ==========================================
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        antiSpamCooldown.remove(event.getPlayer().getUniqueId());
        globalCounters.remove(event.getPlayer().getUniqueId().toString());
    }
}
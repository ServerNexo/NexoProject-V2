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
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏰 NexoDungeons - Listener y Motor de Puzzles (Arquitectura Enterprise)
 * Rendimiento: Virtual Threads Puros, Schedulers Folia-Ready y FastAsyncWorldEdit (FAWE).
 */
@Singleton
public class DungeonListener implements Listener {

    private final NexoDungeons plugin;
    private final PuzzleEngine puzzleEngine;
    private final WaveManager waveManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    private final Map<UUID, Long> antiSpamCooldown = new ConcurrentHashMap<>();
    private final Map<String, Integer> globalCounters = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public DungeonListener(NexoDungeons plugin, PuzzleEngine puzzleEngine, 
                           WaveManager waveManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.puzzleEngine = puzzleEngine;
        this.waveManager = waveManager;
        this.crossplayUtils = crossplayUtils;
    }

    // =========================================
    // 🖱️ 1. INTERACTUAR
    // =========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
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
    // 🧠 3. EL CEREBRO DEL MOTOR
    // =========================================
    private void procesarEventoMotor(Player p, org.bukkit.block.Block block, String triggerType, org.bukkit.event.Cancellable bukkitEvent) {
        var rule = puzzleEngine.getRuleAt(block.getLocation());
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

        // 🚀 JAVA 21 VIRTUAL THREADS: Offload del procesamiento de acciones fuera del Main Thread
        Thread.startVirtualThread(() -> {
            for (var action : rule.actions()) {
                ejecutarAccion(p, action, block.getLocation(), rule);
            }
        });
    }

    // =========================================
    // 🎬 4. EJECUTOR DE ACCIONES (Hilo Virtual)
    // =========================================
    private void ejecutarAccion(Player p, EventRule.Action action, Location baseLoc, EventRule rule) {
        try {
            switch (action.type().toUpperCase()) {

                // 🔊 Reproducir Sonido
                case "PLAY_SOUND" -> {
                    float volume = action.volume() != null ? action.volume().floatValue() : 1.0f;
                    float pitch = action.pitch() != null ? action.pitch().floatValue() : 1.0f;

                    String soundKey = action.sound().toLowerCase().replace("_", ".");
                    var sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundKey));

                    if (sound != null) {
                        // 🌟 FOLIA NATIVE: Sincroniza desde el Hilo Virtual al Hilo de la Región (Chunk)
                        Bukkit.getRegionScheduler().run(plugin, baseLoc, task -> baseLoc.getWorld().playSound(baseLoc, sound, volume, pitch));
                    } else {
                        plugin.getLogger().warning("⚠️ El sonido '" + action.sound() + "' no se encontró en el registro de Minecraft.");
                    }
                }

                // 📦 Consumir Ítem
                case "CONSUME_NEXO_ITEM" -> {
                    // 🌟 FOLIA NATIVE: Sincroniza desde el Hilo Virtual al Hilo del Jugador (Inventario)
                    p.getScheduler().run(plugin, task -> {
                        boolean consumed = false;
                        String targetId = action.itemId();
                        int amountNeeded = action.amount() != null ? action.amount() : 1;

                        for (var item : p.getInventory().getContents()) {
                            // 🌟 PAPER FIX: isEmpty() nativo
                            if (item != null && !item.isEmpty() && item.getType().name().equalsIgnoreCase(targetId)) {
                                if (item.getAmount() >= amountNeeded) {
                                    item.setAmount(item.getAmount() - amountNeeded);
                                    consumed = true;
                                    break;
                                }
                            }
                        }
                        if (!consumed) {
                            // 🌟 Uso de la dependencia inyectada
                            crossplayUtils.sendMessage(p, "&#FF5555[!] No posees el artefacto necesario para activar este mecanismo.");
                        }
                    }, null);
                }

                // 🐉 Invocar Boss
                case "SPAWN_MYTHICMOB" -> {
                    String mobId = action.mobId();

                    int offsetX = rule.isInstanced() ? (baseLoc.getBlockX() - rule.trigger().loc().get("x")) : 0;
                    int offsetZ = rule.isInstanced() ? (baseLoc.getBlockZ() - rule.trigger().loc().get("z")) : 0;

                    double x = action.loc().getOrDefault("x", baseLoc.getBlockX()) + offsetX;
                    double y = action.loc().getOrDefault("y", baseLoc.getBlockY());
                    double z = action.loc().getOrDefault("z", baseLoc.getBlockZ()) + offsetZ;
                    var spawnLoc = new Location(baseLoc.getWorld(), x, y, z);

                    // 🌟 FOLIA NATIVE: Sincroniza al hilo de la coordenada exacta donde nacerá el Boss
                    Bukkit.getRegionScheduler().run(plugin, spawnLoc, task -> {
                        io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().spawnMob(mobId, spawnLoc);
                    });
                }

                // 🧱 REEMPLAZAR BLOQUES CON FAWE (FastAsyncWorldEdit)
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

                    int x1 = loc1.get("x") + offsetX;
                    int y1 = loc1.get("y") + offsetY;
                    int z1 = loc1.get("z") + offsetZ;

                    int x2 = loc2.get("x") + offsetX;
                    int y2 = loc2.get("y") + offsetY;
                    int z2 = loc2.get("z") + offsetZ;

                    var material = Material.valueOf(action.material().toUpperCase());
                    var weBlock = BukkitAdapter.adapt(material.createBlockData());
                    var weWorld = BukkitAdapter.adapt(baseLoc.getWorld());

                    // 🌟 FAWE en Virtual Threads: Las EditSession son lock-free y asíncronas de forma nativa.
                    // Mantener esto dentro del hilo virtual es el pico absoluto de rendimiento.
                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                        var region = new CuboidRegion(weWorld, BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));
                        editSession.setBlocks(region, weBlock);
                    }
                }

                // ⚔️ Iniciar Oleadas
                case "START_WAVE_ARENA" -> {
                    String arenaId = action.counterId();
                    // 🌟 FOLIA NATIVE: Salto al RegionScheduler
                    Bukkit.getRegionScheduler().run(plugin, baseLoc, task -> waveManager.startArena(arenaId, baseLoc));
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
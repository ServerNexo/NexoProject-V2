package me.nexo.dungeons.grid;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🏰 NexoDungeons - Generador de Cuadrículas FAWE (Arquitectura Enterprise Java 21)
 * Rendimiento: Virtual Threads para I/O, FAWE Lock-Free y Cero Estáticos.
 */
@Singleton
public class DungeonGridManager {

    private final NexoDungeons plugin;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    private static final String DUNGEON_WORLD_NAME = "nexo_dungeons";
    private static final int SLOT_DISTANCE = 10000; // 10k bloques entre cada mazmorra
    
    private final AtomicInteger currentSlot = new AtomicInteger(1);
    
    // 🌟 JAVA 21: Motor de Hilos Virtuales para operaciones I/O masivas (Lectura de Schematics)
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private World dungeonWorld;
    private final File schematicsFolder;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public DungeonGridManager(NexoDungeons plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");

        setupVoidWorld();

        // Crear carpeta de schematics si no existe
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    // 🌌 1. Crea el mundo del Vacío puro
    @SuppressWarnings({"deprecation", "removal"}) // Mantenemos para el ChunkGenerator vacío
    private void setupVoidWorld() {
        var creator = new WorldCreator(DUNGEON_WORLD_NAME);
        creator.generator(new ChunkGenerator() {}); // Vacío absoluto, sin lag de generación
        this.dungeonWorld = Bukkit.createWorld(creator);

        if (dungeonWorld != null) {
            dungeonWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            dungeonWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            dungeonWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            dungeonWorld.setTime(18000); // Oscuridad temática
        } else {
            plugin.getLogger().severe("❌ FATAL: No se pudo generar el mundo de instancias de mazmorras.");
        }
    }

    // 📐 2. Calcula el siguiente "Slot" lejano de forma atómica (Thread-Safe)
    public Location getNextSlotLocation() {
        int slot = currentSlot.getAndIncrement();
        return new Location(dungeonWorld, slot * SLOT_DISTANCE, 64, 0);
    }

    // 🏗️ 3. Pega la mazmorra con la potencia de FAWE y Virtual Threads
    public CompletableFuture<Location> pasteDungeonAsync(String schematicName) {
        // 🌟 FIX RENDIMIENTO: Delegamos el I/O al motor de Hilos Virtuales (No al ForkJoinPool)
        return CompletableFuture.supplyAsync(() -> {
            var schemFile = new File(schematicsFolder, schematicName + ".schem");

            if (!schemFile.exists()) {
                plugin.getLogger().severe("❌ Schematic no encontrado: " + schematicName + ".schem");
                return null;
            }

            var pasteLoc = getNextSlotLocation();
            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);

            if (format == null) {
                plugin.getLogger().severe("❌ Formato de archivo irreconocible para: " + schematicName);
                return null;
            }

            // Operación I/O segura: Leer el archivo. El Virtual Thread se desmontará aquí si el disco es lento.
            try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                Clipboard clipboard = reader.read();

                // Intercepción de FAWE: EditSession ultra rápida
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(dungeonWorld))) {
                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(BlockVector3.at(pasteLoc.getBlockX(), pasteLoc.getBlockY(), pasteLoc.getBlockZ()))
                            .ignoreAirBlocks(false)
                            .build();

                    Operations.complete(operation);
                    plugin.getLogger().info("✅ Instancia [" + schematicName + "] generada en X:" + pasteLoc.getBlockX());
                    return pasteLoc;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error construyendo la mazmorra: " + e.getMessage());
                return null;
            }
        }, ioExecutor); // 🌟 Inyectamos el Virtual Thread Executor aquí
    }

    // 🧹 4. SISTEMA DE APAGADO (Ejecutado desde el onDisable)
    public void clearActiveDungeons() {
        if (dungeonWorld != null) {
            // 🌟 Fallback seguro para el mundo principal
            var safeWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (safeWorld == null) return;
            
            var safeLoc = safeWorld.getSpawnLocation(); 
            
            for (var player : dungeonWorld.getPlayers()) {
                // 🌟 Teletransporte seguro de Paper
                player.teleportAsync(safeLoc);
                // 🌟 Utilidad inyectada
                crossplayUtils.sendMessage(player, "&#FF5555[!] Las mazmorras han colapsado repentinamente debido a una fluctuación en el Vacío (Reinicio del Servidor).");
            }
            plugin.getLogger().info("🏰 Todas las instancias han sido evacuadas.");
        }
    }
}
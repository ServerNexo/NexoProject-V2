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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🏰 NexoDungeons - Generador de Cuadrículas FAWE (Arquitectura Enterprise)
 */
@Singleton
public class DungeonGridManager {

    private final NexoDungeons plugin;
    private final String DUNGEON_WORLD_NAME = "nexo_dungeons";
    private final int SLOT_DISTANCE = 10000; // 10k bloques entre cada mazmorra
    private final AtomicInteger currentSlot = new AtomicInteger(1);

    private World dungeonWorld;
    private final File schematicsFolder;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public DungeonGridManager(NexoDungeons plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");

        setupVoidWorld();

        // Crear carpeta de schematics si no existe
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    // 🌌 1. Crea el mundo del Vacío puro
    @SuppressWarnings({"deprecation", "removal"}) // Silenciamos advertencias de Paper
    private void setupVoidWorld() {
        WorldCreator creator = new WorldCreator(DUNGEON_WORLD_NAME);
        creator.generator(new ChunkGenerator() {}); // Vacío absoluto, sin lag de generación
        this.dungeonWorld = Bukkit.createWorld(creator);

        if (dungeonWorld != null) {
            dungeonWorld.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            dungeonWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            dungeonWorld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
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

    // 🏗️ 3. Pega la mazmorra con la potencia de FAWE Asíncrono
    public CompletableFuture<Location> pasteDungeonAsync(String schematicName) {
        return CompletableFuture.supplyAsync(() -> {
            // 🌟 FIX: Separación segura de rutas sin importar el Sistema Operativo
            File schemFile = new File(schematicsFolder, schematicName + ".schem");

            if (!schemFile.exists()) {
                plugin.getLogger().severe("❌ Schematic no encontrado: " + schematicName + ".schem");
                return null;
            }

            Location pasteLoc = getNextSlotLocation();
            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);

            if (format == null) {
                plugin.getLogger().severe("❌ Formato de archivo irreconocible para: " + schematicName);
                return null;
            }

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
        });
    }

    // 🧹 4. SISTEMA DE APAGADO (El método que faltaba para el onDisable)
    public void clearActiveDungeons() {
        if (dungeonWorld != null) {
            Location safeLoc = Bukkit.getWorlds().get(0).getSpawnLocation(); // Mundo principal
            for (Player player : dungeonWorld.getPlayers()) {
                // 🌟 Teletransporte seguro de Paper
                player.teleportAsync(safeLoc);
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Las mazmorras han colapsado repentinamente debido a una fluctuación en el Vacío (Reinicio del Servidor).");
            }
            plugin.getLogger().info("🏰 Todas las instancias han sido evacuadas.");
        }
    }
}
package me.nexo.dungeons.grid;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.NexoPasterService; // 🌟 NUESTRO MOTOR NATIVO
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🏰 NexoDungeons - Generador de Cuadrículas Nativo (NexoPaster)
 * Rendimiento: Cero dependencias externas (Sin FAWE). Usa hilos asíncronos nativos de Paper.
 */
@Singleton
public class DungeonGridManager {

    private final NexoDungeons plugin;
    private final CrossplayUtils crossplayUtils;
    private final NexoPasterService pasterService; // 🌟 INYECTADO DESDE EL CORE

    private static final String DUNGEON_WORLD_NAME = "nexo_dungeons";
    private static final int SLOT_DISTANCE = 10000; // 10k bloques entre cada mazmorra

    private final AtomicInteger currentSlot = new AtomicInteger(1);
    private World dungeonWorld;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public DungeonGridManager(NexoDungeons plugin, CrossplayUtils crossplayUtils, NexoPasterService pasterService) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
        this.pasterService = pasterService;

        setupVoidWorld();
        // Nota: Ya no necesitamos crear la carpeta "schematics" aquí,
        // porque NexoPasterService usa la carpeta global "NexoCore/templates"
    }

    // 🌌 1. Crea el mundo del Vacío puro
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

    // 🏗️ 3. Pega la mazmorra usando NexoPaster (Archivos .nbt nativos)
    public CompletableFuture<Location> pasteDungeonAsync(String templateName) {
        Location pasteLoc = getNextSlotLocation();

        // Llamamos al motor nativo del Core (esperará un archivo en NexoCore/templates/)
        return pasterService.pasteTemplateAsync(templateName, pasteLoc).thenApply(success -> {
            if (success) {
                plugin.getLogger().info("✅ Instancia [" + templateName + "] generada en X:" + pasteLoc.getBlockX());
                return pasteLoc;
            } else {
                plugin.getLogger().severe("❌ Error: No se encontró la mazmorra " + templateName + ".nbt");
                return null;
            }
        });
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
                crossplayUtils.sendMessage(player, "&#FF5555[!] Las mazmorras han colapsado repentinamente debido a una fluctuación en el Vacío.");
            }
            plugin.getLogger().info("🏰 Todas las instancias han sido evacuadas.");
        }
    }
}
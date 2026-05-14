package me.aeroxis.mechanics.gathering.world;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.mechanics.AeroxisMechanics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ♻️ Motor de Regeneración (Folia-Ready & Crash-Proof)
 */
@Singleton
public class RegenEngine {

    private final AeroxisMechanics plugin;
    private final Map<String, String> pendingBlocks = new ConcurrentHashMap<>();
    private final File backupFile;
    private final Gson gson = new Gson();

    @Inject
    public RegenEngine(AeroxisMechanics plugin) {
        this.plugin = plugin;
        this.backupFile = new File(plugin.getDataFolder(), "gathering_regen_backup.json");

        loadAndRestoreOnStartup();
        startBackupTask();
    }

    /**
     * Marca un bloque como minado. Lo transforma y programa su regeneración.
     */
    public void markForRegen(Location loc, Material originalMat, Material depletedMat, int regenTicks) {
        // 1. Convertimos la location a un String simple (world,x,y,z) para el JSON
        String locStr = serializeLoc(loc);
        pendingBlocks.put(locStr, originalMat.name());

        // 2. Cambiamos el bloque físicamente
        loc.getBlock().setType(depletedMat);

        // 3. Programamos la tarea nativa de Folia en su propia región
        Bukkit.getRegionScheduler().runDelayed(plugin, loc, task -> {
            regenerateBlock(loc, locStr, originalMat);
        }, regenTicks);
    }

    private void regenerateBlock(Location loc, String locStr, Material originalMat) {
        // Si sigue en la lista, lo regeneramos
        if (pendingBlocks.remove(locStr) != null) {
            loc.getBlock().setType(originalMat);
            // 🌟 FIX: Partícula corregida a HAPPY_VILLAGER
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.5, 0.5), 5);
        }
    }

    // ==========================================
    // 🛡️ SISTEMA CRASH-PROOF (JSON Backup)
    // ==========================================

    private void startBackupTask() {
        // Guarda el mapa en disco cada 10 segundos asíncronamente
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            if (pendingBlocks.isEmpty()) {
                if (backupFile.exists()) backupFile.delete();
                return;
            }
            try (FileWriter writer = new FileWriter(backupFile)) {
                gson.toJson(pendingBlocks, writer);
            } catch (Exception e) {
                plugin.getLogger().warning("Error guardando regen_backup.json");
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void loadAndRestoreOnStartup() {
        if (!backupFile.exists()) return;

        try (FileReader reader = new FileReader(backupFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> loaded = gson.fromJson(reader, type);

            if (loaded != null && !loaded.isEmpty()) {
                plugin.getLogger().info("⚠️ Recuperando " + loaded.size() + " bloques tras un reinicio/crasheo...");

                for (Map.Entry<String, String> entry : loaded.entrySet()) {
                    Location loc = deserializeLoc(entry.getKey());
                    Material mat = Material.valueOf(entry.getValue());

                    if (loc != null && loc.getWorld() != null) {
                        // En el arranque, disparamos tareas inmediatas para restaurar
                        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                            loc.getBlock().setType(mat);
                        });
                    }
                }
            }
            backupFile.delete(); // Limpiamos el archivo tras restaurar
        } catch (Exception e) {
            plugin.getLogger().severe("Error leyendo regen_backup.json: " + e.getMessage());
        }
    }

    // ==========================================
    // 🛑 APAGADO SEGURO (Llamado desde el Bootstrap)
    // ==========================================
    public void forceBackupNow() {
        if (pendingBlocks.isEmpty()) {
            if (backupFile.exists()) backupFile.delete();
            return;
        }
        try (FileWriter writer = new FileWriter(backupFile)) {
            gson.toJson(pendingBlocks, writer);
            plugin.getLogger().info("✅ Copia de seguridad de zonas guardada exitosamente.");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error forzando el guardado de regen_backup.json: " + e.getMessage());
        }
    }

    // Utilidades de Serialización Segura
    private String serializeLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLoc(String str) {
        String[] parts = str.split(",");
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        return new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}
package me.nexo.islas.instances;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.loaders.file.FileLoader;
import me.nexo.core.NexoPasterService;
import me.nexo.core.api.schematics.NexoSchematic; // 🌟 IMPORTACIÓN AÑADIDA
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData; // 🌟 IMPORTACIÓN AÑADIDA
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🌴 Motor de Islas Persistentes - AdvancedSlimePaper API v4
 * Rendimiento: Hilos Virtuales y Sincronización Triple (Bukkit -> SlimeRAM -> Disco).
 * Evita bloqueos del servidor y asegura la persistencia total de los bloques.
 */
@Singleton
public class IslandSlimeManager {

    private final NexoIslas plugin;
    private final NexoPasterService pasterService;
    private final AdvancedSlimePaperAPI slimeAPI;
    private final SlimeLoader fileLoader;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 🌟 Caché Obligatoria: Necesitamos el objeto nativo para crear el archivo .slime
    private final Map<UUID, SlimeWorld> loadedSlimeWorlds = new ConcurrentHashMap<>();

    @Inject
    public IslandSlimeManager(NexoIslas plugin, NexoPasterService pasterService) {
        this.plugin = plugin;
        this.pasterService = pasterService;
        this.slimeAPI = AdvancedSlimePaperAPI.instance();

        File slimeFolder = new File(plugin.getDataFolder().getParentFile(), "slime_worlds");
        if (!slimeFolder.exists()) {
            slimeFolder.mkdirs();
        }
        this.fileLoader = new FileLoader(slimeFolder);
    }

    public CompletableFuture<World> loadOrGenerateIsland(UUID islandId) {
        String worldName = "island_" + islandId.toString();

        World activeWorld = Bukkit.getWorld(worldName);
        if (activeWorld != null) {
            return CompletableFuture.completedFuture(activeWorld);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                SlimeWorld islandToLoad;
                if (fileLoader.worldExists(worldName)) {
                    islandToLoad = slimeAPI.readWorld(fileLoader, worldName, false, new SlimePropertyMap());
                } else {
                    SlimeWorld template = slimeAPI.readWorld(fileLoader, "island_template", true, new SlimePropertyMap());
                    // 🌟 FIX MAESTRO: Pasamos el fileLoader para que NO sea un mundo efímero de RAM
                    islandToLoad = template.clone(worldName, fileLoader);
                }

                loadedSlimeWorlds.put(islandId, islandToLoad);
                return islandToLoad;

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error leyendo archivo Slime: " + e.getMessage());
                return null;
            }
        }, virtualExecutor).thenApply(slimeWorld -> {
            if (slimeWorld == null) return null;

            try {
                CompletableFuture<World> syncLoad = new CompletableFuture<>();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        slimeAPI.loadWorld(slimeWorld, true);
                        World bukkitWorld = Bukkit.getWorld(slimeWorld.getName());
                        // ¡No usamos setAutoSave aquí para no pelear con PaperMC!
                        syncLoad.complete(bukkitWorld);
                    } catch (Exception e) {
                        plugin.getLogger().severe("❌ Error interno de Slime al cargar el mundo: " + e.getMessage());
                        syncLoad.complete(null);
                    }
                });

                return syncLoad.join();

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error inyectando Isla en RAM: " + e.getMessage());
                return null;
            }
        });
    }

    // 🌟 GESTIÓN NORMAL: (Cuando te vas al spawn sin apagar el servidor)
    public void unloadIsland(UUID islandId) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = Bukkit.getWorld("island_" + islandId.toString());
            SlimeWorld slimeWorld = loadedSlimeWorlds.get(islandId);

            if (world != null && slimeWorld != null && world.getPlayers().isEmpty()) {
                plugin.getLogger().info("💾 [1/3] Pasando bloques de Bukkit a SlimePaper...");

                // 🌟 FIX ADVERTENCIAS: Silenciamos el Auto-Save para que Paper no se queje
                boolean wasAutoSave = world.isAutoSave();
                world.setAutoSave(false);
                world.save();
                world.setAutoSave(wasAutoSave);

                virtualExecutor.submit(() -> {
                    try {
                        plugin.getLogger().info("💾 [2/3] Escribiendo archivo .slime en el disco...");
                        slimeAPI.saveWorld(slimeWorld);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.unloadWorld(world, false);
                            loadedSlimeWorlds.remove(islandId);
                            plugin.getLogger().info("✅ [3/3] Isla " + islandId + " guardada y descargada exitosamente.");
                        });

                    } catch (IOException e) {
                        plugin.getLogger().severe("❌ Error escribiendo el archivo .slime: " + e.getMessage());
                    }
                });
            }
        }, 60L); // 3 segundos de gracia para estabilizar I/O y teleportaciones
    }

    // 🛑 APAGADO DE EMERGENCIA (/STOP): Guardado Triple Forzoso y Síncrono
    public void unloadIslandSyncForShutdown(UUID islandId) {
        World world = Bukkit.getWorld("island_" + islandId.toString());
        SlimeWorld slimeWorld = loadedSlimeWorlds.get(islandId);

        plugin.getLogger().info("🕵️‍♂️ Iniciando protocolo de apagado seguro para: " + islandId);

        if (world != null && slimeWorld != null) {

            if (!world.getPlayers().isEmpty()) {
                Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
                for (Player p : world.getPlayers()) {
                    p.teleport(fallback); // Expulsión síncrona obligatoria
                }
            }

            plugin.getLogger().info("💾 [1/3] Sincronizando Bukkit con Slime...");
            boolean wasAutoSave = world.isAutoSave();
            world.setAutoSave(false);
            world.save();
            world.setAutoSave(wasAutoSave);

            try {
                plugin.getLogger().info("💾 [2/3] Guardando archivo .slime...");
                slimeAPI.saveWorld(slimeWorld); // Bloquea el servidor hasta que termine de escribir en el disco duro

                Bukkit.unloadWorld(world, false);
                loadedSlimeWorlds.remove(islandId);
                plugin.getLogger().info("✅ [3/3] Isla blindada y guardada.");
            } catch (IOException e) {
                plugin.getLogger().severe("❌ Error crítico de I/O al apagar: " + e.getMessage());
            }

        } else {
            plugin.getLogger().warning("⚠️ El mundo ya estaba guardado o no existía.");
        }
    }

    public void upgradeIslandBorders(IslandProfile profile, World islandWorld) {
        CompletableFuture.runAsync(() -> {
            int currentBorderTier = profile.getBorderLevel();
            int radioFisico = profile.getRealBorderSize();

            Bukkit.getScheduler().runTask(plugin, () -> {
                islandWorld.getWorldBorder().setSize(radioFisico * 2);
            });

            Location pasteLoc = new Location(islandWorld, 0.5, 50, 0.5);

            // 🌟 FIX: Usamos NexoSchematic en lugar del String (Aquí creamos un dummy vacío para compilar)
            // En un futuro, deberías leer esto de una caché real: SchematicCache.get("island_ring_upgrade_tier_" + currentBorderTier)
            BlockData[] emptyData = new BlockData[0];
            NexoSchematic ringSchematic = new NexoSchematic("island_ring_upgrade_tier_" + currentBorderTier, 0, 0, 0, emptyData);

            pasterService.pasteAsynchronously(ringSchematic, pasteLoc);

        }, virtualExecutor);
    }

    public void importarPlantillaVanilla() {
        virtualExecutor.submit(() -> {
            try {
                plugin.getLogger().info("⏳ Importando 'island_template' a formato Slime...");
                SlimeWorld template = slimeAPI.readVanillaWorld(new File("."), "island_template", fileLoader);
                slimeAPI.saveWorld(template);
                plugin.getLogger().info("✅ ¡Plantilla importada a Slime con éxito!");
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error importando la plantilla: " + e.getMessage());
            }
        });
    }
}
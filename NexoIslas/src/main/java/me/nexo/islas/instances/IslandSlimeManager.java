package me.nexo.islas.instances;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.loaders.file.FileLoader;
import me.nexo.core.NexoPasterService;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🌴 Motor de Islas Persistentes - AdvancedSlimePaper API v4
 * Rendimiento: Hilos Virtuales, World ID por Island_UUID, y Bordes basados en Upgrade Points.
 */
@Singleton
public class IslandSlimeManager {

    private final NexoIslas plugin;
    private final NexoPasterService pasterService;
    private final AdvancedSlimePaperAPI slimeAPI;
    private final SlimeLoader fileLoader;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public IslandSlimeManager(NexoIslas plugin, NexoPasterService pasterService) {
        this.plugin = plugin;
        this.pasterService = pasterService;

        // Asignación de la API
        this.slimeAPI = AdvancedSlimePaperAPI.instance();

        // Usamos java.io.File para SlimePaper
        File slimeFolder = new File(plugin.getDataFolder().getParentFile(), "slime_worlds");
        if (!slimeFolder.exists()) {
            slimeFolder.mkdirs();
        }
        this.fileLoader = new FileLoader(slimeFolder);
    }

    // 🌟 FIX AUDITORÍA: Separamos la lectura Asíncrona (Disco) de la carga Síncrona (RAM)
    public CompletableFuture<World> loadOrGenerateIsland(UUID islandId) {
        String worldName = "island_" + islandId.toString();

        // 🌟 FIX CRÍTICO: Si el mundo ya está cargado en la RAM de Bukkit, lo devolvemos inmediatamente.
        // Esto evita el crasheo silencioso al poner /is home por segunda vez.
        World activeWorld = Bukkit.getWorld(worldName);
        if (activeWorld != null) {
            return CompletableFuture.completedFuture(activeWorld);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 🚀 FASE 1 (HILO VIRTUAL): Lectura del disco y clonación pesada (Zero Lag)
                SlimeWorld islandToLoad;
                if (fileLoader.worldExists(worldName)) {
                    islandToLoad = slimeAPI.readWorld(fileLoader, worldName, false, new SlimePropertyMap());
                } else {
                    SlimeWorld template = slimeAPI.readWorld(fileLoader, "island_template", true, new SlimePropertyMap());
                    islandToLoad = template.clone(worldName);
                }
                return islandToLoad;

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error leyendo archivo Slime: " + e.getMessage());
                return null;
            }
        }, virtualExecutor).thenApply(slimeWorld -> {
            // 🛑 SI HUBO ERROR O NO SE ENCONTRÓ LA PLANTILLA, CANCELAR
            if (slimeWorld == null) return null;

            // 🚀 FASE 2 (HILO PRINCIPAL): Inyección a la memoria RAM de Bukkit
            // El loadWorld() *debe* correr en el hilo principal de Paper
            try {
                // Creamos un Future para esperar el resultado síncrono
                CompletableFuture<World> syncLoad = new CompletableFuture<>();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        slimeAPI.loadWorld(slimeWorld, true);
                        syncLoad.complete(Bukkit.getWorld(slimeWorld.getName()));
                    } catch (Exception e) {
                        plugin.getLogger().severe("❌ Error interno de Slime al cargar el mundo: " + e.getMessage());
                        syncLoad.complete(null);
                    }
                });

                // Esperamos y devolvemos el Mundo cargado (Es seguro usar join en un hilo virtual)
                return syncLoad.join();

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error inyectando Isla en RAM: " + e.getMessage());
                return null;
            }
        });
    }

    // 🌟 FIX: Descarga usando el islandId
    public void unloadIsland(UUID islandId) {
        String worldName = "island_" + islandId.toString();
        World world = Bukkit.getWorld(worldName);
        if (world != null && world.getPlayers().isEmpty()) {
            Bukkit.unloadWorld(world, true); // Guarda los cambios en el disco local y libera RAM
        }
    }

    /**
     * Expande los límites físicos de la isla basado en el nivel de mejora de borde del perfil.
     * Llamado cada vez que un jugador compra el Upgrade de tamaño.
     */
    public void upgradeIslandBorders(IslandProfile profile, World islandWorld) {
        CompletableFuture.runAsync(() -> {
            // 🌟 FIX ENTERPRISE: Usamos el nivel de mejora de bordes (borderLevel)
            int currentBorderTier = profile.getBorderLevel();
            int radioFisico = profile.getRealBorderSize(); // Ej: Tier 1=50, Tier 2=100, Tier 3=150

            // Expansión del borde virtual
            islandWorld.getWorldBorder().setSize(radioFisico * 2);

            // Generación física del anillo de expansión (Opcional, si tienes schematics)
            // NexoPasterService se encargará de pegarlo asíncronamente sin lag
            Location pasteLoc = new Location(islandWorld, 0.5, 50, 0.5);
            pasterService.pasteTemplateAsync("island_ring_upgrade_tier_" + currentBorderTier, pasteLoc);

        }, virtualExecutor);
    }

    /**
     * 🌟 IMPORTADOR NATIVO: Convierte la carpeta del mundo Bukkit a formato .slime
     * Ejecutar solo una vez para crear la plantilla desde cero.
     */
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
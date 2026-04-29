package me.nexo.islas.instances;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.loaders.file.FileLoader; // 🌟 Ya no dará error tras el refresh de Gradle
import me.nexo.core.NexoPasterService;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File; // 🌟 IMPORTANTE: Necesario para el FileLoader
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🌴 Motor de Islas Persistentes - AdvancedSlimePaper API v4
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

        // 🌟 MAGIA ASP V4: Asignación de la API
        this.slimeAPI = AdvancedSlimePaperAPI.instance();

        // 🌟 FIX V4: Usamos java.io.File como dicta el código fuente de SlimePaper
        File slimeFolder = new File(plugin.getDataFolder().getParentFile(), "slime_worlds");
        if (!slimeFolder.exists()) {
            slimeFolder.mkdirs(); // Crea la carpeta si no existe
        }
        this.fileLoader = new FileLoader(slimeFolder);
    }

    public CompletableFuture<World> loadOrGenerateIsland(UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> {
            String worldName = "island_" + ownerId.toString();
            try {
                if (fileLoader.worldExists(worldName)) {
                    // 1. LEER EL MUNDO DEL DISCO
                    SlimeWorld island = slimeAPI.readWorld(fileLoader, worldName, false, new SlimePropertyMap());

                    // 2. CARGARLO EN LA RAM DE BUKKIT (El 'true' hace la magia en V4)
                    slimeAPI.loadWorld(island, true);

                    return Bukkit.getWorld(worldName);
                } else {
                    // 1. LEER LA PLANTILLA Y CLONARLA
                    SlimeWorld template = slimeAPI.readWorld(fileLoader, "island_template", true, new SlimePropertyMap());
                    SlimeWorld newIsland = template.clone(worldName);

                    // 2. CARGAR LA NUEVA ISLA EN LA RAM DE BUKKIT
                    slimeAPI.loadWorld(newIsland, true);

                    return Bukkit.getWorld(worldName);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando Isla: " + e.getMessage());
                return null;
            }
        }, virtualExecutor);
    }

    public void unloadIsland(UUID ownerId) {
        String worldName = "island_" + ownerId.toString();
        World world = Bukkit.getWorld(worldName);
        if (world != null && world.getPlayers().isEmpty()) {
            Bukkit.unloadWorld(world, true); // Guarda los cambios en el disco local y libera RAM
        }
    }

    public void upgradeIslandBorders(IslandProfile profile, World islandWorld) {
        CompletableFuture.runAsync(() -> {
            int nivelRiqueza = (int) profile.getWealthScore();
            int radioFisico = 50 + (nivelRiqueza * 10);

            // Expansión del borde virtual
            islandWorld.getWorldBorder().setSize(radioFisico * 2);

            // Generación física del anillo de expansión
            Location pasteLoc = new Location(islandWorld, 0, 50, 0);
            pasterService.pasteTemplateAsync("island_ring_upgrade_tier_" + nivelRiqueza, pasteLoc);

        }, virtualExecutor);
    }
}
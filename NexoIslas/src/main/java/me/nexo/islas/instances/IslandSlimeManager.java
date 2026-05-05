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

    // 🌟 FIX: Ahora recibimos el UUID de la ISLA (islandId), no el del dueño.
    public CompletableFuture<World> loadOrGenerateIsland(UUID islandId) {
        return CompletableFuture.supplyAsync(() -> {
            String worldName = "island_" + islandId.toString();
            try {
                if (fileLoader.worldExists(worldName)) {
                    // 1. LEER EL MUNDO DEL DISCO
                    SlimeWorld island = slimeAPI.readWorld(fileLoader, worldName, false, new SlimePropertyMap());

                    // 2. CARGARLO EN LA RAM DE BUKKIT
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
                plugin.getLogger().severe("❌ Error cargando Isla física: " + e.getMessage());
                return null;
            }
        }, virtualExecutor);
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
}
package me.aeroxis.dungeons.instances;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.loaders.file.FileLoader; // 🌟 File Loader de V4
import me.aeroxis.core.AeroxisPasterService;
import me.aeroxis.core.api.schematics.AeroxisSchematic; // 🌟 IMPORTACIÓN AÑADIDA
import me.aeroxis.dungeons.AeroxisDungeons;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material; // 🌟 IMPORTACIÓN AÑADIDA
import org.bukkit.World;
import org.bukkit.block.data.BlockData; // 🌟 IMPORTACIÓN AÑADIDA
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏰 Motor de Instancias Efímeras - AdvancedSlimePaper API v4
 * Usa Java 21+ Virtual Threads para carga/descarga asíncrona en RAM.
 */
@Singleton
public class DungeonSlimeManager {

    private final AeroxisDungeons plugin;
    private final AeroxisPasterService pasterService;
    private final AdvancedSlimePaperAPI slimeAPI;
    private final SlimeLoader fileLoader;

    // 🚀 JAVA 21+: Virtual Threads Executor para concurrencia masiva
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<UUID, String> activeDungeonWorlds = new ConcurrentHashMap<>();

    @Inject
    public DungeonSlimeManager(AeroxisDungeons plugin, AeroxisPasterService pasterService) {
        this.plugin = plugin;
        this.pasterService = pasterService;

        // 🌟 MAGIA ASP V4: Instancia directa
        this.slimeAPI = AdvancedSlimePaperAPI.instance();

        // 🌟 FIX V4: Instanciamos el FileLoader manualmente apuntando a la misma carpeta
        File slimeFolder = new File(plugin.getDataFolder().getParentFile(), "slime_worlds");
        if (!slimeFolder.exists()) {
            slimeFolder.mkdirs();
        }
        this.fileLoader = new FileLoader(slimeFolder);
    }

    /**
     * Clona una plantilla y la carga en RAM.
     */
    public CompletableFuture<World> createDungeonInstance(UUID partyId, String templateName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String instanceName = "dungeon_" + partyId.toString();

                // 🌟 FIX: En ASP V4 se usa el método universal 'setValue'
                SlimePropertyMap props = new SlimePropertyMap();
                props.setValue(SlimeProperties.DIFFICULTY, "hard");
                props.setValue(SlimeProperties.PVP, false);

                // 1. LEER LA PLANTILLA DEL DISCO (Con las nuevas propiedades aplicadas)
                SlimeWorld template = slimeAPI.readWorld(fileLoader, templateName, true, props);

                // 2. CLONAR LA PLANTILLA
                SlimeWorld instance = template.clone(instanceName);

                // 3. CARGAR EN RAM DE BUKKIT (El 'true' hace la magia en V4)
                slimeAPI.loadWorld(instance, true);

                activeDungeonWorlds.put(partyId, instanceName);

                return Bukkit.getWorld(instanceName);
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error instanciando Dungeon en RAM: " + e.getMessage());
                return null;
            }
        }, virtualExecutor);
    }

    /**
     * Elimina el mundo de la RAM al terminar (Sin guardar, totalmente efímero).
     */
    public void destroyDungeonInstance(UUID partyId) {
        String worldName = activeDungeonWorlds.remove(partyId);
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                // Expulsar jugadores rezagados
                for (Player p : world.getPlayers()) {
                    p.teleportAsync(Bukkit.getWorlds().get(0).getSpawnLocation());
                    p.sendMessage("§cLa instancia de la mazmorra ha colapsado.");
                }

                // 🌟 false = NO GUARDAR EN DISCO. Destruye la instancia para siempre liberando la RAM
                Bukkit.unloadWorld(world, false);
            }
        }
    }

    /**
     * Genera el Loot usando NexoPaster asíncronamente (UX Premium)
     */
    public void spawnBossLoot(UUID partyId, Location bossDeathLoc) {
        CompletableFuture.runAsync(() -> {
            // 🌟 FIX: Usamos el método moderno de AeroxisPasterService con Location directa y esquemática en RAM
            // Creamos un cofre falso de 1x1x1 para que compile (Luego se leerá de tu caché)
            BlockData[] chestData = new BlockData[]{Bukkit.createBlockData(Material.CHEST)};
            AeroxisSchematic lootSchematic = new AeroxisSchematic("dungeon_tier1_chest", 1, 1, 1, chestData);

            pasterService.pasteAsynchronously(lootSchematic, bossDeathLoc);

            // UX Auditiva
            for (Player p : bossDeathLoc.getWorld().getPlayers()) {
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                p.sendMessage("§x§0§0§e§6§f§f✨ ¡El jefe ha caído! El botín ha aparecido.");
            }
        }, virtualExecutor);
    }
}
package me.aeroxis.core.hub;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.AeroxisPasterService;
import me.aeroxis.core.api.schematics.AeroxisSchematic;
import me.aeroxis.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material; // 🌟 IMPORTACIÓN AÑADIDA
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData; // 🌟 IMPORTACIÓN AÑADIDA
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ AeroxisCore - Gestor de Reconstrucción del Hub (Folia-Ready)
 * Maneja el progreso comunitario y dispara la construcción de los edificios.
 */
@Singleton
public class HubDonationManager {

    private final AeroxisCore plugin;
    private final AeroxisPasterService pasterService;
    private final CrossplayUtils crossplayUtils;

    // 🌟 Modelo de Datos de un Proyecto
    public static class HubProject {
        public String id;
        public String displayName;
        public int requiredAmount;
        public int currentAmount;
        public AeroxisSchematic schematic; // 🌟 FIX: Ahora usa el formato rápido en memoria
        public Location buildLocation;
        public boolean isCompleted;

        public HubProject(String id, String displayName, int requiredAmount, AeroxisSchematic schematic, Location buildLocation) {
            this.id = id;
            this.displayName = displayName;
            this.requiredAmount = requiredAmount;
            this.schematic = schematic;
            this.buildLocation = buildLocation;
            this.currentAmount = 0;
            this.isCompleted = false;
        }
    }

    // ⚡ Memoria Concurrente de Proyectos Activos
    private final Map<String, HubProject> activeProjects = new ConcurrentHashMap<>();

    @Inject
    public HubDonationManager(AeroxisCore plugin, AeroxisPasterService pasterService, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.pasterService = pasterService;
        this.crossplayUtils = crossplayUtils;

        loadDummyProjects();
    }

    /**
     * Registra un aporte de un jugador a un proyecto específico.
     */
    public boolean addDonation(Player player, String projectId, int amount) {
        HubProject project = activeProjects.get(projectId);

        if (project == null || project.isCompleted) {
            crossplayUtils.sendMessage(player, "&#FF5555❌ Este proyecto ya no acepta donaciones.");
            return false;
        }

        int remaining = project.requiredAmount - project.currentAmount;
        int actualDonation = Math.min(amount, remaining);

        project.currentAmount += actualDonation;

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        crossplayUtils.sendMessage(player, "&#55FF55[!] Has donado " + actualDonation + " recursos a la " + project.displayName + ".");

        if (project.currentAmount >= project.requiredAmount) {
            completeProject(project);
        }

        return true;
    }

    /**
     * 🏗️ EVENTO CLÍMAX: La comunidad ha terminado el proyecto.
     */
    private void completeProject(HubProject project) {
        project.isCompleted = true;

        crossplayUtils.broadcastMessage("\n&#FFD700<bold>🏛 ¡PROYECTO COMUNITARIO COMPLETADO! 🏛</bold>");
        crossplayUtils.broadcastMessage("&#E6CCFFLa comunidad ha logrado reunir los recursos para: &#55FF55" + project.displayName);
        crossplayUtils.broadcastMessage("&#E6CCFF¡Iniciando construcción automática!\n");

        // 🌟 FIX: Usamos el nuevo motor asíncrono pasteAsynchronously y .thenRun()
        pasterService.pasteAsynchronously(project.schematic, project.buildLocation).thenRun(() -> {

            // 🌟 FIX FOLIA: Los fuegos artificiales (Entidades) deben ir en el RegionScheduler del edificio
            Bukkit.getRegionScheduler().execute(plugin, project.buildLocation, () -> {
                spawnCelebration(project.buildLocation);
            });

        });
    }

    /**
     * Genera fuegos artificiales alrededor del nuevo edificio.
     */
    private void spawnCelebration(Location loc) {
        for (int i = 0; i < 5; i++) {
            Location fwLoc = loc.clone().add(Math.random() * 10 - 5, 2, Math.random() * 10 - 5);
            Firework fw = (Firework) loc.getWorld().spawnEntity(fwLoc, EntityType.FIREWORK_ROCKET);
            FireworkMeta fwm = fw.getFireworkMeta();

            fwm.addEffect(FireworkEffect.builder().withColor(Color.YELLOW).withFade(Color.ORANGE).with(FireworkEffect.Type.BALL_LARGE).trail(true).build());
            fwm.setPower(1);
            fw.setFireworkMeta(fwm);
        }
    }

    public HubProject getProject(String id) {
        return activeProjects.get(id);
    }

    /**
     * Carga un proyecto de prueba.
     */
    private void loadDummyProjects() {
        World world = Bukkit.getWorld("world");
        if (world != null) {

            // 🌟 FIX: Creamos un AeroxisSchematic de prueba en memoria (Un bloque de Yunques de 3x3x3)
            int size = 3;
            BlockData[] dummyBlocks = new BlockData[size * size * size];
            BlockData anvilData = Bukkit.createBlockData(Material.ANVIL);
            java.util.Arrays.fill(dummyBlocks, anvilData);

            AeroxisSchematic herreriaSchem = new AeroxisSchematic("herreria_t2", size, size, size, dummyBlocks);

            HubProject herreria = new HubProject("herreria_t2", "Herrería Nivel 2", 10000, herreriaSchem, new Location(world, 50, 65, 50));
            activeProjects.put(herreria.id, herreria);
        }
    }
}
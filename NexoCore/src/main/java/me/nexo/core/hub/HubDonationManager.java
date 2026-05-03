package me.nexo.core.hub;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.NexoPasterService;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World; // 🌟 FIX: IMPORTACIÓN FALTANTE AÑADIDA
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ NexoCore - Gestor de Reconstrucción del Hub
 * Maneja el progreso comunitario y dispara la construcción de los edificios.
 */
@Singleton
public class HubDonationManager {

    private final NexoCore plugin;
    private final NexoPasterService pasterService;
    private final CrossplayUtils crossplayUtils;

    // 🌟 Modelo de Datos de un Proyecto
    public static class HubProject {
        public String id;
        public String displayName;
        public int requiredAmount;
        public int currentAmount;
        public String templateName; // El archivo .nbt
        public Location buildLocation;
        public boolean isCompleted;

        public HubProject(String id, String displayName, int requiredAmount, String templateName, Location buildLocation) {
            this.id = id;
            this.displayName = displayName;
            this.requiredAmount = requiredAmount;
            this.templateName = templateName;
            this.buildLocation = buildLocation;
            this.currentAmount = 0;
            this.isCompleted = false;
        }
    }

    // ⚡ Memoria Concurrente de Proyectos Activos
    private final Map<String, HubProject> activeProjects = new ConcurrentHashMap<>();

    @Inject
    public HubDonationManager(NexoCore plugin, NexoPasterService pasterService, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.pasterService = pasterService;
        this.crossplayUtils = crossplayUtils;

        // Cargar proyectos desde una base de datos o config.yml
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

        // Evitamos que donen de más
        int remaining = project.requiredAmount - project.currentAmount;
        int actualDonation = Math.min(amount, remaining);

        project.currentAmount += actualDonation;

        // Feedback al jugador
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        crossplayUtils.sendMessage(player, "&#55FF55[!] Has donado " + actualDonation + " recursos a la " + project.displayName + ".");

        // 🌟 ¿Se alcanzó la meta?
        if (project.currentAmount >= project.requiredAmount) {
            completeProject(project);
        }

        // TODO: Guardar el nuevo progreso en config.yml o DB para que no se pierda al reiniciar
        return true;
    }

    /**
     * 🏗️ EVENTO CLÍMAX: La comunidad ha terminado el proyecto.
     */
    private void completeProject(HubProject project) {
        project.isCompleted = true;

        // 1. Anuncio Global Épico
        crossplayUtils.broadcastMessage("\n&#FFD700<bold>🏛 ¡PROYECTO COMUNITARIO COMPLETADO! 🏛</bold>");
        crossplayUtils.broadcastMessage("&#E6CCFFLa comunidad ha logrado reunir los recursos para: &#55FF55" + project.displayName);
        crossplayUtils.broadcastMessage("&#E6CCFF¡Iniciando construcción automática!\n");

        // 2. Disparamos la inyección del .nbt asíncronamente
        pasterService.pasteTemplateAsync(project.templateName, project.buildLocation).thenAccept(success -> {
            if (success) {
                // Volvemos al Main Thread para lanzar los fuegos artificiales
                Bukkit.getScheduler().runTask(plugin, () -> {
                    spawnCelebration(project.buildLocation);
                });
            }
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
     * Carga un proyecto de prueba para poder testearlo de inmediato.
     */
    private void loadDummyProjects() {
        World world = Bukkit.getWorld("world");
        if (world != null) {
            // Un proyecto que requiere 10,000 de oro, usa la plantilla "herreria_nbt" y se pega en X:50, Y:65, Z:50
            HubProject herreria = new HubProject("herreria_t2", "Herrería Nivel 2", 10000, "herreria_nbt", new Location(world, 50, 65, 50));
            activeProjects.put(herreria.id, herreria);
        }
    }
}
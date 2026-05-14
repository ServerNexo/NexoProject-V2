package me.aeroxis.core.cataclysms;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ☄️ AeroxisCore - Motor de Cataclismos (Lluvia de Meteoritos)
 * Genera eventos dinámicos en Nodos Predefinidos del Spawn.
 */
@Singleton
public class CataclysmManager {

    private final AeroxisCore plugin;
    private final CrossplayUtils crossplayUtils;

    private final List<Location> crashNodes = new ArrayList<>();

    // Estado del Meteorito
    private Location activeMeteorLocation;
    private int meteorHealth = 0;
    private static final int MAX_HEALTH = 50;
    private static final int DESPAWN_TIME_TICKS = 20 * 60 * 15; // 15 Minutos

    // 🌟 ADN del Meteorito (Para resetear los límites de los jugadores)
    private UUID currentMeteorId = UUID.randomUUID();

    @Inject
    public CataclysmManager(AeroxisCore plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;

        loadCrashNodes();
    }

    private void loadCrashNodes() {
        // TODO: Leer desde config.yml
        World world = Bukkit.getWorld("world");
        if (world != null) {
            crashNodes.add(new Location(world, 0.5, 65, 0.5));
            crashNodes.add(new Location(world, 10.5, 65, 15.5));
            crashNodes.add(new Location(world, -12.5, 65, -8.5));
        }
    }

    public void triggerRandomMeteorite() {
        if (crashNodes.isEmpty()) {
            plugin.getLogger().warning("❌ No hay Nodos de Impacto configurados.");
            return;
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(crashNodes.size());
        Location targetLocation = crashNodes.get(randomIndex);

        spawnMeteorite(targetLocation);
    }

    private void spawnMeteorite(Location loc) {
        if (activeMeteorLocation != null) {
            clearMeteorite(false);
        }

        this.activeMeteorLocation = loc;
        this.meteorHealth = MAX_HEALTH;
        this.currentMeteorId = UUID.randomUUID(); // 🌟 Nuevo ADN para el nuevo meteorito

        World world = loc.getWorld();
        if (world == null) return;

        world.strikeLightningEffect(loc);
        world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0.5, 1, 0.5), 10, 2, 2, 2, 0.1);

        Block coreBlock = loc.getBlock();
        coreBlock.setType(Material.CRYING_OBSIDIAN);

        crossplayUtils.broadcastMessage("\n&#FF5555<bold>☄ ¡ALERTA DE CATACLISMO!</bold>");
        crossplayUtils.broadcastMessage("&#E6CCFFUn Fragmento Estelar ha impactado cerca de las coordenadas: &#55FF55X: " + loc.getBlockX() + ", Z: " + loc.getBlockZ());
        crossplayUtils.broadcastMessage("&#E6CCFFEstráiganlo antes de que se evapore.\n");

        startParticleTask(loc);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isMeteorActive() && loc.equals(activeMeteorLocation)) {
                clearMeteorite(true);
            }
        }, DESPAWN_TIME_TICKS);
    }

    public boolean isMeteorActive() {
        return activeMeteorLocation != null;
    }

    public boolean isMeteorBlock(Block block) {
        return isMeteorActive() && block.getLocation().equals(activeMeteorLocation);
    }

    public void damageMeteor() {
        meteorHealth--;
        if (meteorHealth <= 0) {
            clearMeteorite(false);
            crossplayUtils.broadcastMessage("\n&#55FF55<bold>✨ EVENTO FINALIZADO</bold>");
            crossplayUtils.broadcastMessage("&#E6CCFFEl Meteorito ha sido agotado por completo.\n");
        }
    }

    private void clearMeteorite(boolean timeout) {
        if (activeMeteorLocation != null) {
            Block block = activeMeteorLocation.getBlock();
            if (block.getType() == Material.CRYING_OBSIDIAN) {
                block.setType(Material.AIR);
                block.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, activeMeteorLocation.clone().add(0.5, 0.5, 0.5), 20, 0.5, 0.5, 0.5, 0.05);
                block.getWorld().playSound(activeMeteorLocation, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
            }
            if (timeout) {
                crossplayUtils.broadcastMessage("&#777777El Meteorito se ha evaporado...");
            }
            activeMeteorLocation = null;
        }
    }

    private void startParticleTask(Location loc) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            if (!isMeteorActive() || !loc.equals(activeMeteorLocation)) {
                task.cancel();
                return;
            }
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0.5, 1.2, 0.5), 3, 0.3, 0.3, 0.3, 0.01);
            loc.getWorld().spawnParticle(Particle.ASH, loc.clone().add(0.5, 1.0, 0.5), 5, 0.5, 0.5, 0.5, 0);
        }, 0L, 10L);
    }

    // 🌟 GETTER DEL ADN
    public UUID getCurrentMeteorId() {
        return currentMeteorId;
    }
}
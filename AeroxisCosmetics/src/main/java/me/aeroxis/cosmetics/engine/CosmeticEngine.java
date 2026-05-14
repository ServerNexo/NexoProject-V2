package me.aeroxis.cosmetics.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.cosmetics.manager.CosmeticManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.concurrent.TimeUnit;

/**
 * ⚙️ AeroxisCosmetics - Motor de Partículas y Mascotas
 * Rendimiento Extremo: Matemáticas Asíncronas y Entity Schedulers.
 */
@Singleton
public class CosmeticEngine {

    private final JavaPlugin plugin;
    private final CosmeticManager cosmeticManager;

    @Inject
    public CosmeticEngine(JavaPlugin plugin, CosmeticManager cosmeticManager) {
        this.plugin = plugin;
        this.cosmeticManager = cosmeticManager;
    }

    public void startEngines() {
        // Motor de Alas: Cada 4 ticks (200ms)
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> wingsEngine(), 50, 200, TimeUnit.MILLISECONDS);

        // 🌟 NUEVO: Motor de Halos: Cada 2 ticks (100ms)
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> halosEngine(), 50, 100, TimeUnit.MILLISECONDS);

        // Motor de Mascotas: Cada 2 ticks (100ms)
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> petsEngine(), 50, 100, TimeUnit.MILLISECONDS);

        if (Bukkit.getPluginManager().getPlugin("AeroxisIslas") != null) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> bordersEngine(), 50, 1000, TimeUnit.MILLISECONDS);
        }
    }

    private void wingsEngine() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            var cosmetics = cosmeticManager.getActiveCosmetics(p.getUniqueId());
            if (cosmetics == null || cosmetics.wingsEffect() == null) continue;

            // 🌟 6 TIPOS DE ALAS DINÁMICAS
            Particle particleType = switch (cosmetics.wingsEffect()) {
                case "wings_angel" -> Particle.END_ROD;
                case "wings_demon" -> Particle.SMOKE;
                case "wings_fire" -> Particle.FLAME;
                case "wings_fairy" -> Particle.CHERRY_LEAVES;
                case "wings_void" -> Particle.PORTAL;
                case "wings_frost" -> Particle.SNOWFLAKE;
                default -> Particle.FLAME;
            };

            Location loc = p.getLocation();
            double yaw = Math.toRadians(loc.getYaw());
            double cos = Math.cos(yaw);
            double sin = Math.sin(yaw);

            // 🌟 DISEÑO AAA: Matriz de 22 puntos para alas con volumen y curvatura
            double[][] offsets = {
                    // Ala Izquierda
                    {-0.2, 1.2, -0.2}, {-0.4, 1.4, -0.25}, {-0.6, 1.6, -0.3}, {-0.8, 1.7, -0.35},
                    {-1.0, 1.8, -0.4}, {-1.2, 1.85, -0.45}, {-1.4, 1.8, -0.5},
                    {-1.1, 1.5, -0.45}, {-0.8, 1.3, -0.4}, {-0.5, 1.1, -0.3}, {-0.3, 0.9, -0.2},

                    // Ala Derecha
                    {0.2, 1.2, -0.2}, {0.4, 1.4, -0.25}, {0.6, 1.6, -0.3}, {0.8, 1.7, -0.35},
                    {1.0, 1.8, -0.4}, {1.2, 1.85, -0.45}, {1.4, 1.8, -0.5},
                    {1.1, 1.5, -0.45}, {0.8, 1.3, -0.4}, {0.5, 1.1, -0.3}, {0.3, 0.9, -0.2}
            };

            for (double[] offset : offsets) {
                double rotatedX = offset[0] * cos - offset[2] * sin;
                double rotatedZ = offset[0] * sin + offset[2] * cos;
                Location particleLoc = loc.clone().add(rotatedX, offset[1], rotatedZ);

                // Velocidad 0.0 para evitar dispersión, garantizando la forma geométrica
                particleLoc.getWorld().spawnParticle(particleType, particleLoc, 1, 0, 0, 0, 0.0);
            }
        }
    }

    // 🌟 NUEVO MOTOR: HALOS
    private void halosEngine() {
        double time = System.currentTimeMillis() / 1000.0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            var cosmetics = cosmeticManager.getActiveCosmetics(p.getUniqueId());
            if (cosmetics == null || cosmetics.haloEffect() == null) continue;

            Location headLoc = p.getLocation().add(0, 2.2, 0); // Posicionamos justo sobre la cabeza

            for (int i = 0; i < 10; i++) { // Dibuja un anillo de 10 puntos girando
                double angle = (2 * Math.PI * i / 10) + time * 2; // "time * 2" define la velocidad de rotación
                double x = 0.4 * Math.cos(angle);
                double z = 0.4 * Math.sin(angle);
                Location pLoc = headLoc.clone().add(x, 0, z);

                switch (cosmetics.haloEffect()) {
                    case "halo_divine" -> pLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, pLoc, 1, 0, 0, 0, 0.0);
                    case "halo_cursed" -> pLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, 0, 0, 0, 0.0);
                    case "halo_king" -> pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(org.bukkit.Color.YELLOW, 0.6f));
                }
            }
        }
    }

    private void petsEngine() {
        double time = System.currentTimeMillis() / 1000.0;
        double radius = 1.5;

        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemDisplay pet = cosmeticManager.getPet(p.getUniqueId());
            if (pet == null || pet.isDead()) continue;

            float x = (float) (Math.cos(time) * radius);
            float z = (float) (Math.sin(time) * radius);

            Transformation transform = new Transformation(
                    new Vector3f(x, 1.8f, z),
                    new AxisAngle4f(),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new AxisAngle4f()
            );

            pet.getScheduler().execute(plugin, () -> {
                pet.setInterpolationDuration(3);
                pet.setInterpolationDelay(0);
                pet.setTransformation(transform);
            }, null, 1L);
        }
    }

    private void bordersEngine() {
        // Reservado para la API de Islas (AeroxisIslas)
    }
}
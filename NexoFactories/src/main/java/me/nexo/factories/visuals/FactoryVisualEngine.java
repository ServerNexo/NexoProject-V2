package me.nexo.factories.visuals;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.factories.NexoFactories;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * 🏭 NexoFactories - Motor de Físicas Visuales (Arquitectura AAA)
 * Rendimiento: Cero ticks de servidor. Usa Interpolación de Cliente para animar la producción.
 */
@Singleton
public class FactoryVisualEngine {

    private final NexoFactories plugin;

    @Inject
    public FactoryVisualEngine(NexoFactories plugin) {
        this.plugin = plugin;
    }

    /**
     * Ejecuta una animación de "Cinta Transportadora" donde el ítem sale de la máquina
     * y se mueve en el aire, calculado 100% por la tarjeta gráfica del jugador.
     */
    public void playProductionAnimation(Location coreLoc, ItemStack generatedItem) {
        // 🌟 REGLA DE PAPER 1.21+: Tocar el mundo debe hacerse en el Hilo de la Región (Folia-Ready)
        Bukkit.getRegionScheduler().execute(plugin, coreLoc, () -> {

            // Aparece justo arriba del bloque de la máquina
            Location spawnLoc = coreLoc.clone().add(0.5, 1.2, 0.5);

            // Efectos industriales iniciales (Humo y sonido mecánico)
            spawnLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, spawnLoc, 5, 0.2, 0.1, 0.2, 0.05);
            spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_COPPER_HIT, 0.5f, 1.5f);

            // Invocamos la Display Entity
            spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
                display.setItemStack(generatedItem);
                display.setBillboard(ItemDisplay.Billboard.FIXED); // Fijo para rotaciones 3D perfectas

                // 🌟 MAGIA AAA: Preparamos la animación del cliente (40 Ticks = 2 Segundos)
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(40);

                // Calculamos el movimiento
                Transformation trans = display.getTransformation();

                // Movemos el ítem +1.2 bloques hacia adelante (Z) y lo hacemos caer ligeramente (-0.3 en Y)
                trans.getTranslation().add(0.0f, -0.3f, 1.2f);

                // Le damos un giro completo sobre su propio eje vertical
                trans.getRightRotation().set(new AxisAngle4f((float) Math.PI * 2, new Vector3f(0, 1, 0)));

                // ¡Disparamos la animación al cliente!
                display.setTransformation(trans);

                // Limpiamos la entidad EXACTAMENTE cuando la animación termina
                display.getScheduler().runDelayed(plugin, task -> {
                    if (display.isValid()) {
                        // Partícula de cuando el ítem "entra" al almacenamiento final
                        display.getWorld().spawnParticle(Particle.CRIT, display.getLocation(), 5, 0.1, 0.1, 0.1, 0);
                        display.remove();
                    }
                }, null, 40L);
            });
        });
    }
}
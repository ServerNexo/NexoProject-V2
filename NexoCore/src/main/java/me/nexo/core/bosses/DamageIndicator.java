package me.nexo.core.bosses;

import me.nexo.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;

import java.util.Random;

/**
 * ✨ Indicadores de Daño Flotantes (Cero Lag Visual y Componentes Kyori)
 */
public class DamageIndicator {

    private static final Random random = new Random();

    // 🌟 FIX: Añadimos CrossplayUtils a los parámetros para mantener compatibilidad
    public static void spawn(JavaPlugin plugin, CrossplayUtils crossplayUtils, Location loc, double damage, boolean critical) {
        // Hacemos que el número flote un poco alrededor del jefe
        Location spawnLoc = loc.clone().add(
                (random.nextDouble() - 0.5) * 1.5,
                random.nextDouble() * 1.5,
                (random.nextDouble() - 0.5) * 1.5
        );

        TextDisplay textDisplay = (TextDisplay) loc.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);

        // Formateo del daño
        String color = critical ? "&#FF5555" : "&#FFAA00";
        String prefix = critical ? "✧ " : "";
        String formattedDamage = String.format("%.1f", damage);

        // 🌟 FIX: Convertimos el String a un Component de Kyori usando tu utilidad
        Component textoFinal = crossplayUtils.parseCrossplay(null, color + prefix + formattedDamage + (critical ? " ✧" : ""));

        // Usamos el método moderno .text()
        textDisplay.text(textoFinal);

        // Ajustes de rendimiento y visuales
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setDefaultBackground(false);
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        textDisplay.setShadowed(true);

        // Animación pequeña hacia arriba
        Transformation transform = textDisplay.getTransformation();
        transform.getTranslation().add(0, 0.5f, 0);
        textDisplay.setInterpolationDuration(20);
        textDisplay.setTransformation(transform);

        // Se elimina a sí mismo después de 1 segundo (20 ticks)
        plugin.getServer().getScheduler().runTaskLater(plugin, textDisplay::remove, 20L);
    }
}
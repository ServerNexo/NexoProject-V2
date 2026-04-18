package me.nexo.core.crossplay;

import org.bukkit.Location;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

/**
 * 📱 Optimizador de Displays (Paper 26.1+)
 * Resuelve problemas nativos de interpolación de Bedrock y sincroniza Hitboxes.
 */
public class NexoDisplayFixer {

    /**
     * 🛡️ Interaction Bridge para Modelos 3D.
     */
    public static Interaction spawnBedrockHitbox(ItemDisplay display, float width, float height, boolean makePassenger) {
        Location loc = display.getLocation();

        // 🌟 GENERACIÓN ATÓMICA: Paper permite configurar TODO (incluso pasajeros) antes de enviar el paquete de spawn al jugador.
        return loc.getWorld().spawn(loc, Interaction.class, interaction -> {
            interaction.setInteractionWidth(width);
            interaction.setInteractionHeight(height);
            interaction.setResponsive(true);

            if (makePassenger) {
                display.addPassenger(interaction); // Cero parpadeo visual (Zero-tick lag)
            }
        });
    }

    /**
     * 📱 Anti-Jitter para Hologramas de Bedrock.
     */
    public static void applyAntiJitter(TextDisplay hologram) {
        hologram.setBillboard(TextDisplay.Billboard.CENTER);

        // Cero milisegundos de interpolación erradica el temblor en móviles
        hologram.setInterpolationDuration(0);
        hologram.setInterpolationDelay(0);

        // Optimizamos las sombras que causan caídas de FPS en Android/iOS
        hologram.setShadowRadius(0f);
        hologram.setShadowStrength(0f);

        // 🌟 PAPER API EXTRA: Reducimos el ViewRange para que móviles no rendericen textos a más de 32 bloques
        hologram.setViewRange(0.5f);
    }
}
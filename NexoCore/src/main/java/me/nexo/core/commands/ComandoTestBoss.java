package me.nexo.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.bosses.NexoBoss; // 🌟 IMPORTAMOS LA CLASE BASE
import me.nexo.core.bosses.NexoBossRegistry; // 🌟 IMPORTAMOS LA API
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.visuals.MobVisualManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ Nexo Network - Comando TestBoss (Arquitectura Enterprise)
 * Invoca jefes dinámicamente usando el Registry para mantener el Core desacoplado.
 */
@Singleton
public final class ComandoTestBoss { // 🌟 'final' por seguridad arquitectónica

    private final NexoCore plugin;
    private final MobVisualManager mobVisualManager;
    private final CrossplayUtils crossplayUtils;
    private final NexoBossRegistry bossRegistry; // 🌟 AÑADIMOS EL REGISTRO

    @Inject
    public ComandoTestBoss(NexoCore plugin, MobVisualManager mobVisualManager, CrossplayUtils crossplayUtils, NexoBossRegistry bossRegistry) {
        this.plugin = plugin;
        this.mobVisualManager = mobVisualManager;
        this.crossplayUtils = crossplayUtils;
        this.bossRegistry = bossRegistry;
    }

    // 🌟 FIX: Cambiado de "testboss" a "nexoboss"
    @Command("nexoboss")
    @CommandPermission("nexocore.commands.admin")
    public void invocarTestBoss(Player player) {
        try {
            crossplayUtils.sendMessage(player, "&#FFAA00[🔧] Consultando el Registry de Jefes...");

            // Calculamos la posición: 3 bloques adelante, sobre la superficie
            Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(3));
            loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);

            // 🌟 MAGIA ENTERPRISE: Le pedimos a la API que busque la receta de "EL_RENACIDO" y lo cree
            NexoBoss jefe = bossRegistry.spawnBoss("EL_RENACIDO", plugin, loc, 100);

            // Verificamos que se haya creado correctamente (podría ser null si Dungeons no está cargado)
            if (jefe != null && jefe.getEntity() != null) {
                mobVisualManager.attachCustomHologram(jefe.getEntity(), 50);
                crossplayUtils.sendMessage(player, "&#FF3366[!] <bold>Has invocado a un Jefe Global.</bold>");
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.0f);
            } else {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Error Crítico: No se encontró la receta de EL_RENACIDO. ¿Está NexoDungeons activado?");
            }

        } catch (Exception e) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Error de Java al ejecutar el comando. Revisa la consola.");
            e.printStackTrace();
        }
    }
}
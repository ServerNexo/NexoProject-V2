package me.aeroxis.mechanics.lategame.fracture;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisPasterService;
import me.aeroxis.core.api.schematics.AeroxisSchematic;
import me.aeroxis.mechanics.AeroxisMechanics;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ☄️ Motor de Colapso Orbital (NexoFracture)
 * Gestiona la inestabilidad de los asteroides y los destruye asíncronamente usando NexoPaster.
 */
@Singleton
public class AsteroidCollapseEngine {

    private final AeroxisMechanics plugin;
    private final AeroxisPasterService pasterService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public AsteroidCollapseEngine(AeroxisMechanics plugin, AeroxisPasterService pasterService) {
        this.plugin = plugin;
        this.pasterService = pasterService;
    }

    /**
     * ⏱️ Inicia la cuenta regresiva para la destrucción de un asteroide.
     * @param center El centro del asteroide.
     * @param radius El radio en bloques del asteroide (para saber cuánto borrar).
     * @param seconds Tiempo antes de la detonación.
     * @param players Jugadores en la zona que verán la BossBar.
     */
    public void triggerCollapse(Location center, int radius, int seconds, Collection<Player> players) {
        AtomicInteger timeLeft = new AtomicInteger(seconds);
        
        // 1. Crear la BossBar Roja Parpadeante
        BossBar collapseBar = BossBar.bossBar(
                mm.deserialize("<bold><red>⚠ INESTABILIDAD ORBITAL ⚠</red></bold>"),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_20
        );

        // Mostrarla a los mineros cercanos
        for (Player p : players) {
            p.showBossBar(collapseBar);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
        }

        // 2. Temporizador Asíncrono (Folia-Ready)
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            int current = timeLeft.decrementAndGet();
            
            // Actualizar la barra
            float progress = Math.max(0.0f, (float) current / seconds);
            collapseBar.progress(progress);

            // Efectos de tensión en los últimos segundos (RegionScheduler)
            if (current <= 5 && current > 0) {
                Bukkit.getRegionScheduler().execute(plugin, center, () -> {
                    center.getWorld().playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 2.0f, 1.5f);
                    center.getWorld().spawnParticle(Particle.LAVA, center, 50, radius, radius, radius, 0.1);
                });
            }

            // 💥 DETONACIÓN
            if (current <= 0) {
                task.cancel();
                for (Player p : players) p.hideBossBar(collapseBar);
                executeAnnihilation(center, radius);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 🕳️ Borra físicamente el asteroide inyectando una plantilla de "Aire".
     */
    private void executeAnnihilation(Location center, int radius) {
        int diameter = radius * 2;
        
        // 1. Efectos Visuales y Sonoros Masivos
        Bukkit.getRegionScheduler().execute(plugin, center, () -> {
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.5f);
            center.getWorld().spawnParticle(Particle.EXPLOSION, center, 20, diameter/2.0, diameter/2.0, diameter/2.0, 0);
            center.getWorld().spawnParticle(Particle.ASH, center, 500, diameter, diameter, diameter, 0.1);
        });

        // 2. Crear una Plantilla de Vacío (Air Schematic) en la RAM de forma asíncrona
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            BlockData[] airBlocks = new BlockData[diameter * diameter * diameter];
            BlockData airData = Bukkit.createBlockData(Material.AIR);
            Arrays.fill(airBlocks, airData); // Llenamos el array con aire ultra-rápido
            
            AeroxisSchematic voidSchematic = new AeroxisSchematic("void_wipe", diameter, diameter, diameter, airBlocks);

            // 3. 🚀 Llamamos a nuestro NexoPaster para que Folia borre el asteroide Chunk por Chunk sin lag
            pasterService.pasteAsynchronously(voidSchematic, center).thenRun(() -> {
                plugin.getLogger().info("☄️ Asteroide colapsado de forma segura en: " + center.toString());
                // (Opcional) Aquí podrías hacer daño a los jugadores que no escaparon a tiempo
            });
        });
    }
}
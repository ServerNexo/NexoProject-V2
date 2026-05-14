package me.aeroxis.mechanics.lategame.yggdrasil;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.items.api.lategame.LateGameArmorStats; // 🌟 IMPORTADO DE NEXOITEMS
import me.aeroxis.items.api.lategame.LateGameStatsCache; // 🌟 IMPORTADO DE NEXOITEMS
import me.aeroxis.mechanics.AeroxisMechanics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.TimeUnit;

/**
 * 🌪️ Motor de Físicas de Viento (Yggdrasil)
 * Aplica empuje suave constante usando EntitySchedulers en Folia.
 */
@Singleton
public class WindPhysicsEngine {

    private final AeroxisMechanics plugin;
    private final Vector currentWindVector = new Vector(0.08, 0.0, 0.0); // Viento hacia el Este

    @Inject
    public WindPhysicsEngine(AeroxisMechanics plugin) {
        this.plugin = plugin;
        startWindTask();
    }

    private void startWindTask() {
        // Escáner asíncrono maestro cada 2 ticks (100ms)
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getName().equals("yggdrasil_world")) {
                    applyWindPhysics(player);
                }
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void applyWindPhysics(Player player) {
        // 🌟 MAGIA FOLIA: Las físicas de una entidad DEBEN correr en su propio hilo
        player.getScheduler().run(plugin, scheduledTask -> {
            if (player.isDead() || player.getGameMode().name().equals("SPECTATOR")) return;

            // 🌟 LECTURA REAL DE LA ARMADURA DESDE NEXOITEMS
            LateGameArmorStats stats = LateGameStatsCache.getStats(player);

            // Si tiene resistencia al viento y está agachado (Sneak Lock), se ancla a la rama.
            boolean isAnchored = stats.canResistWind() && player.isSneaking();

            if (!isAnchored) {
                // Aplicamos el vector de viento suavemente a la velocidad actual del jugador
                player.setVelocity(player.getVelocity().add(currentWindVector));
            }
        }, null);
    }
}
package me.nexo.core.crossplay;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Collection;

/**
 * ⚡ Optimizador de Partículas Nexo (Paper 26.1+ Nativo)
 * Cero NMS. Evita el AsyncCatcher leyendo en Main y enviando paquetes en Virtual Threads.
 */
public class ParticleOptimizer {

    public static void spawnOptimized(Location loc, Particle particle, int count, double oX, double oY, double oZ, double speed) {
        if (loc.getWorld() == null) return;

        // 🛡️ LECTURA SEGURA O(1): Obtenemos los jugadores en el Main Thread para evitar que Paper crashee (AsyncCatcher).
        Collection<Player> targets = loc.getNearbyPlayers(48);
        if (targets.isEmpty()) return;

        // 🚀 MATEMÁTICAS Y RED: Desplegamos el Hilo Virtual de Java 25 para el envío masivo de paquetes.
        Thread.startVirtualThread(() -> {

            // 🌟 PAPER API: Usamos ParticleBuilder en lugar de spawnParticle. Es infinitamente más rápido y no genera lag de GC.
            var javaBuilder = new ParticleBuilder(particle).location(loc).count(count).offset(oX, oY, oZ).extra(speed);

            Particle bedrockSafeParticle = getLighterParticle(particle);
            int bedrockCount = Math.max(1, (int) (count * 0.3));
            var bedrockBuilder = new ParticleBuilder(bedrockSafeParticle).location(loc).count(bedrockCount).offset(oX, oY, oZ).extra(speed);

            for (Player player : targets) {
                if (!player.isOnline()) continue;

                // Floodgate check
                if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                    bedrockBuilder.receivers(player).spawn(); // Envía paquete nativo al cliente Bedrock
                } else {
                    javaBuilder.receivers(player).spawn(); // Envía paquete nativo al cliente Java
                }
            }
        });
    }

    /**
     * Convierte partículas masivas de humo o fuego expansivo (Causantes de Lag Spikes en móviles)
     * en indicadores de bajo consumo.
     */
    private static Particle getLighterParticle(Particle p) {
        // 🌟 Switch Expression (Java moderno) - Las partículas han sido validadas para 1.21.4+
        return switch (p) {
            case FLAME, LAVA, CAMPFIRE_COSY_SMOKE, SMOKE -> Particle.CRIT;
            case EXPLOSION_EMITTER, EXPLOSION -> Particle.FIREWORK;
            case DRAGON_BREATH -> Particle.WITCH;
            case SWEEP_ATTACK -> Particle.DAMAGE_INDICATOR;
            default -> p;
        };
    }
}
package me.nexo.core.crossplay;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ⚡ Nexo Network - Optimizador de Partículas (Arquitectura Enterprise)
 * Cero NMS. Evita el AsyncCatcher leyendo en Main y enviando paquetes a través de un pool de Virtual Threads.
 */
@Singleton
public class ParticleOptimizer {

    // 🚀 Motor de concurrencia administrado (Evita hilos fantasma)
    private final ExecutorService virtualExecutor;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ParticleOptimizer() {
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void spawnOptimized(Location loc, Particle particle, int count, double oX, double oY, double oZ, double speed) {
        if (loc.getWorld() == null) return;

        // 🛡️ LECTURA SEGURA O(1): Obtenemos los jugadores en el Main Thread para evitar que Paper crashee (AsyncCatcher).
        Collection<Player> targets = loc.getNearbyPlayers(48);
        if (targets.isEmpty()) return;

        // 🚀 MATEMÁTICAS Y RED: Desplegamos el Hilo Virtual manejado para el envío masivo de paquetes.
        virtualExecutor.submit(() -> {

            // 🌟 PAPER API: Usamos ParticleBuilder en lugar de spawnParticle. Es infinitamente más rápido.
            var javaBuilder = new ParticleBuilder(particle).location(loc).count(count).offset(oX, oY, oZ).extra(speed);

            Particle bedrockSafeParticle = getLighterParticle(particle);
            int bedrockCount = Math.max(1, (int) (count * 0.3));
            var bedrockBuilder = new ParticleBuilder(bedrockSafeParticle).location(loc).count(bedrockCount).offset(oX, oY, oZ).extra(speed);

            for (Player player : targets) {
                if (!player.isOnline()) continue;

                // Floodgate check (Llamada a API externa segura)
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
    private Particle getLighterParticle(Particle p) {
        // 🌟 Switch Expression (Java 21+) - Las partículas han sido validadas para 1.21.5
        return switch (p) {
            case FLAME, LAVA, CAMPFIRE_COSY_SMOKE, SMOKE -> Particle.CRIT;
            case EXPLOSION_EMITTER, EXPLOSION -> Particle.FIREWORK;
            case DRAGON_BREATH -> Particle.WITCH;
            case SWEEP_ATTACK -> Particle.DAMAGE_INDICATOR;
            default -> p;
        };
    }
}
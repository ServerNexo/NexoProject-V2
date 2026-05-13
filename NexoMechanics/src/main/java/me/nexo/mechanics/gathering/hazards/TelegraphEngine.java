package me.nexo.mechanics.gathering.hazards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.gathering.data.Profession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ⚡ Motor de Telegrafía (Nodos Inestables)
 * Da a los jugadores 2 segundos para retroceder antes de la detonación.
 */
@Singleton
public class TelegraphEngine {

    private final NexoMechanics plugin;
    private final HazardDispatcher dispatcher;
    
    // Mapa seguro para hilos: Rastrea qué bloques están a punto de explotar
    private final Map<Location, ActiveTelegraph> unstableNodes = new ConcurrentHashMap<>();

    @Inject
    public TelegraphEngine(NexoMechanics plugin, HazardDispatcher dispatcher) {
        this.plugin = plugin;
        this.dispatcher = dispatcher;
    }

    /**
     * Intenta registrar un bloque inestable.
     * @return TRUE si el bloque ACABA de volverse inestable (Cierra el evento).
     * FALSE si el bloque ya era inestable (Significa que el jugador lo golpeó de nuevo -> DETONAR).
     */
    public boolean handleUnstableNode(Player player, Location loc, Profession profession) {
        if (unstableNodes.containsKey(loc)) {
            // 💥 EL JUGADOR FUE IMPACIENTE Y LO GOLPEÓ DE NUEVO: Detonación Instantánea
            detonate(player, loc, profession);
            return false; 
        }

        // ⚠️ INICIA LA TELEGRAFÍA
        player.sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<red><bold>¡NODO INESTABLE! ¡ALÉJATE!</bold></red>"));
        
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.5f);

        // Crear y programar la tarea visual de Folia
        TelegraphTask taskLogic = new TelegraphTask(player, loc, profession);
        ScheduledTask scheduledTask = Bukkit.getRegionScheduler().runAtFixedRate(plugin, loc, taskLogic, 1L, 5L); // Ejecuta cada 0.25 seg (5 ticks)
        
        unstableNodes.put(loc, new ActiveTelegraph(profession, scheduledTask));
        return true;
    }

    private void detonate(Player player, Location loc, Profession profession) {
        ActiveTelegraph active = unstableNodes.remove(loc);
        if (active != null) {
            active.task().cancel(); // Detenemos la telegrafía
        }
        dispatcher.dispatch(profession, player, loc);
    }

    // ==========================================
    // 🧠 LÓGICA DE LA TAREA ASÍNCRONA (Folia)
    // ==========================================
    private record ActiveTelegraph(Profession profession, ScheduledTask task) {}

    private class TelegraphTask implements Consumer<ScheduledTask> {
        private final Player player;
        private final Location loc;
        private final Profession prof;
        private int ticks = 0;

        public TelegraphTask(Player player, Location loc, Profession prof) {
            this.player = player;
            this.loc = loc;
            this.prof = prof;
        }

        @Override
        public void accept(ScheduledTask task) {
            ticks++;

            // Efectos visuales según la profesión
            Particle p = switch (prof) {
                case MINING -> Particle.LAVA;
                case WOODCUTTING -> Particle.SQUID_INK;
                case FARMING -> Particle.WHITE_SMOKE;
            };
            
            loc.getWorld().spawnParticle(p, loc.clone().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0.05);
            loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 2.0f);

            // A los 8 ciclos (2 segundos exactos), estalla por sí solo si nadie lo tocó
            if (ticks >= 8) {
                detonate(player, loc, prof);
            }
        }
    }
}
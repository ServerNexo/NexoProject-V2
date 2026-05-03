package me.nexo.dungeons.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.dungeons.instances.DungeonSlimeManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📈 NexoDungeons - Motor Matemático de Escalado Procedural
 * Ajusta la Vida, Daño y Velocidad de los mobs en tiempo real basándose en el Gear Score de la Party.
 */
@Singleton
public class AbyssScalingEngine implements Listener {

    private final DungeonSlimeManager slimeManager;

    // ⚡ Caché concurrente O(1) que guarda el "Gear Score Promedio" de cada Mundo Instanciado
    private final Map<String, Integer> worldGearScoreCache = new ConcurrentHashMap<>();

    @Inject
    public AbyssScalingEngine(DungeonSlimeManager slimeManager) {
        this.slimeManager = slimeManager;
    }

    /**
     * Registra una nueva instancia y calcula el poder del escuadrón.
     * Llamado por el QueueManager al crear la partida.
     */
    public void registerInstance(String worldName, List<Player> party) {
        if (party.isEmpty()) return;

        int totalScore = 0;
        for (Player p : party) {
            totalScore += calculateGearScore(p);
        }

        int avgScore = Math.max(100, totalScore / party.size()); // Base mínima: 100 GS
        worldGearScoreCache.put(worldName, avgScore);
    }

    /**
     * Limpia la memoria cuando la dungeon se destruye.
     */
    public void unregisterInstance(String worldName) {
        worldGearScoreCache.remove(worldName);
    }

    /**
     * Calcula el poder del jugador. 
     * TODO: Conectar con NexoItems para leer Item Data Components de la armadura equipada.
     */
    private int calculateGearScore(Player p) {
        // Fórmula base (Nivel * 2). Se actualizará al inyectar NexoItems.
        return 100 + (p.getLevel() * 2);
    }

    // ========================================================================
    // 🧬 INTERCEPTOR DE SPAWN (Multiplicador Genético)
    // ========================================================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onMobSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        String worldName = entity.getWorld().getName();

        // Solo escalamos monstruos dentro de mundos instanciados del Abismo
        if (!worldName.startsWith("dungeon_") && !worldName.startsWith("inst_")) return;

        int partyGearScore = worldGearScoreCache.getOrDefault(worldName, 100);
        applyMutations(entity, partyGearScore);
    }

    private void applyMutations(LivingEntity entity, int gearScore) {
        // Matemáticas: Si el GS es 200 y la base es 100, el multiplicador es 2.0x
        double multiplier = (double) gearScore / 100.0;
        if (multiplier <= 1.0) return; // Si son nivel bajo, se queda en dificultad normal

        // 1. MUTACIÓN DE VIDA (Escala 1:1)
        AttributeInstance healthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = healthAttr.getBaseValue() * multiplier;
            healthAttr.setBaseValue(newHealth);
            entity.setHealth(newHealth); // Curamos al mob a su nuevo máximo
        }

        // 2. MUTACIÓN DE DAÑO (Escala al 80% para evitar "One-Shots" injustos)
        AttributeInstance damageAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * (multiplier * 0.8));
        }

        // 3. MUTACIÓN DE VELOCIDAD (Escala Reducida: Solo +5% de velocidad por cada 100 GS)
        AttributeInstance speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            double speedMultiplier = 1.0 + ((multiplier - 1.0) * 0.05);
            // Limitamos a 50% de aumento máximo para que no parezcan Flash y rompan las animaciones
            speedAttr.setBaseValue(speedAttr.getBaseValue() * Math.min(speedMultiplier, 1.5));
        }

        // 4. RESISTENCIA AL EMPUJE (Los mobs de alto nivel son más pesados)
        AttributeInstance kbAttr = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            double kbRes = Math.min(0.8, (multiplier - 1.0) * 0.1); // Máximo 80% de resistencia
            kbAttr.setBaseValue(kbAttr.getBaseValue() + kbRes);
        }
    }

    /**
     * 🌟 EXPOSICIÓN DE DATOS PARA EL MOTOR DE BOTÍN
     * Obtiene el Gear Score actual de una instancia.
     * Útil para calcular el botín (Loot) en AbyssLootEngine y otros sistemas externos.
     */
    public int getGearScore(String worldName) {
        return worldGearScoreCache.getOrDefault(worldName, 100);
    }
}
package me.nexo.core.bosses;

import me.nexo.core.bosses.fsm.BossState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey; // 🌟 NUEVO IMPORT
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType; // 🌟 NUEVO IMPORT
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * 👑 NexoBoss - El motor base de IA para todos los jefes AAA.
 */
public abstract class NexoBoss {

    protected final JavaPlugin plugin;
    protected Mob entity;
    protected BossState currentState;
    protected UUID currentTarget;

    // Control de tiempo para la IA
    protected int ticksAlive = 0;
    protected int skillCooldown = 0;

    public NexoBoss(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentState = BossState.SPAWN;
    }

    /**
     * Spawnea al jefe físicamente en el mundo e inicializa su IA y su Etiqueta PDC.
     */
    public void spawn(Location location, int groupGearScore, String internalBossName) {
        // 1. Spawneamos la entidad (Implementado por la clase hija)
        this.entity = spawnPhysicalEntity(location);

        // 🌟 NUEVO: Le inyectamos la etiqueta PDC para que NexoDungeons la lea
        // ✅ Por esto (usando la variable 'plugin' que ya tenemos en la clase):
        NamespacedKey bossKey = new NamespacedKey(plugin, "boss_id");
        this.entity.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, internalBossName);

        // 2. Escalamos sus atributos usando nuestro Scaler
        new BossScaler(plugin).scaleAttributes(this.entity, groupGearScore);

        // 3. Limpiamos su IA nativa de Minecraft para controlarlo nosotros al 100%
        this.entity.setAware(false); // Ignora el pathfinding vanilla por un momento

        // 4. Arrancamos el "Cerebro" (El Loop del FSM)
        startBrain();
    }

    private void startBrain() {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (this.entity == null || this.entity.isDead()) {
                this.currentState = BossState.DEATH;
                onDeath();
                task.cancel();
                return;
            }

            ticksAlive++;
            if (skillCooldown > 0) skillCooldown--;

            // 🧠 LA MÁQUINA DE ESTADOS (FSM)
            switch (currentState) {
                case SPAWN:
                    handleSpawnPhase();
                    break;
                case IDLE:
                    findTarget();
                    break;
                case CHASE:
                    handleChasePhase();
                    break;
                case MELEE_ATTACK:
                    handleMeleeAttack();
                    break;
                case CASTING_SKILL:
                    // Mientras castea, no hace nada más. El método de la skill cambiará el estado al terminar.
                    break;
            }
        }, 0L, 1L); // Corre cada 1 Tick (50ms)
    }

    // ==========================================
    // ⚔️ MÉTODOS QUE DEBEN IMPLEMENTAR LOS JEFES ESPECÍFICOS
    // ==========================================
    protected abstract Mob spawnPhysicalEntity(Location location);
    protected abstract void handleSpawnPhase();
    protected abstract void handleChasePhase();
    protected abstract void handleMeleeAttack();
    protected abstract void onDeath();

    // Función para buscar a quién atacar
    protected void findTarget() {
        // Lógica básica: Buscar al jugador más cercano en 20 bloques
        Player closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Player p : entity.getWorld().getPlayers()) {
            double dist = p.getLocation().distanceSquared(entity.getLocation());
            if (dist < 400 && dist < minDistance) { // 400 es 20 bloques al cuadrado
                closest = p;
                minDistance = dist;
            }
        }

        if (closest != null) {
            this.currentTarget = closest.getUniqueId();
            this.entity.setTarget(closest);
            this.entity.setAware(true); // Permitimos que la IA de Bukkit camine hacia él
            this.currentState = BossState.CHASE;
        }
    }
}
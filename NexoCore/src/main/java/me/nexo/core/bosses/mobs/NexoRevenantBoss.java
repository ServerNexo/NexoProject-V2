package me.nexo.core.bosses.mobs;

import me.nexo.core.bosses.NexoBoss;
import me.nexo.core.bosses.fsm.BossState;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer; // 🌟 NUEVO IMPORT PARA NOMBRES
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 💀 El Renacido (NexoRevenantBoss) - Jefe de Prueba
 * Un esqueleto wither gigante que lanza un ataque de área oscuro.
 */
public class NexoRevenantBoss extends NexoBoss {

    public NexoRevenantBoss(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    protected Mob spawnPhysicalEntity(Location location) {
        // Spawneamos un Wither Skeleton
        Mob boss = (Mob) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);

        // Lo vestimos como un jefe
        boss.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
        boss.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));

        // 🌟 FIX: Usamos Kyori Adventure API en lugar de setCustomName (String) deprecado
        boss.customName(LegacyComponentSerializer.legacyAmpersand().deserialize("&4&lEl Renacido"));

        boss.setCustomNameVisible(true);
        boss.setRemoveWhenFarAway(false);

        return boss;
    }

    @Override
    protected void handleSpawnPhase() {
        // El jefe acaba de aparecer. Esperamos 3 segundos (60 ticks) antes de que ataque.
        if (ticksAlive < 60) {
            // Efecto de partículas asíncrono para que no dé lag
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                entity.getWorld().spawnParticle(Particle.LARGE_SMOKE, entity.getLocation().add(0, 1, 0), 10, 0.5, 1, 0.5, 0.05);
            });
        } else {
            // Terminó la animación de Spawn, pasamos a buscar víctimas
            this.currentState = BossState.IDLE;
        }
    }

    @Override
    protected void handleChasePhase() {
        if (currentTarget == null) {
            this.currentState = BossState.IDLE;
            return;
        }

        Player target = Bukkit.getPlayer(currentTarget);
        if (target == null || !target.isOnline() || target.isDead()) {
            this.currentState = BossState.IDLE; // El jugador murió o escapó, buscar otro
            return;
        }

        double distance = entity.getLocation().distanceSquared(target.getLocation());

        // 1. Si está muy cerca (Ataque Básico)
        if (distance <= 4.0) { // 2 bloques de distancia
            this.currentState = BossState.MELEE_ATTACK;
            return;
        }

        // 2. Si la habilidad especial está lista y está a media distancia
        if (skillCooldown <= 0 && distance < 100.0) { // Menos de 10 bloques
            castShadowSmash(target);
        }
    }

    @Override
    protected void handleMeleeAttack() {
        // Minecraft maneja el golpe físico solo si está cerca, nosotros solo reiniciamos el estado
        // Aquí podríamos añadir un empuje extra o daño personalizado.
        this.currentState = BossState.CHASE;
    }

    @Override
    protected void onDeath() {
        // Explosión de partículas épica al morir (Asíncrona)
        Location loc = entity.getLocation();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 100, 1, 2, 1, 0.1);
            loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
        });

        // Drop de "Esencia de Nexo" (Síncrono, los ítems deben dropearse en el Main Thread)
        loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.NETHER_STAR)); // Aquí pondrás tu ítem custom
    }

    // ==========================================
    // 🔮 HABILIDADES ESPECIALES
    // ==========================================
    private void castShadowSmash(Player target) {
        this.currentState = BossState.CASTING_SKILL;
        this.skillCooldown = 200; // 10 segundos de cooldown
        this.entity.setAware(false); // Se detiene para castear

        Location bossLoc = this.entity.getLocation();
        bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);

        // 🌟 DIBUJO DE SHADERS/PARTÍCULAS EN HILO ASÍNCRONO (CERO LAG)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                Location particleLoc = bossLoc.clone().add(Math.cos(angle) * 3, 0.1, Math.sin(angle) * 3);
                bossLoc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, particleLoc, 5, 0, 0, 0, 0);
            }
        });

        // 💥 DAÑO REAL (Síncrono) - 1.5 segundos después de la animación
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!this.entity.isDead()) {
                // Dañamos a todos en un radio de 4 bloques
                for (org.bukkit.entity.Entity e : this.entity.getNearbyEntities(4, 2, 4)) {
                    if (e instanceof Player p) {
                        p.damage(10.0, this.entity); // 5 Corazones de daño base
                    }
                }

                // Vuelve a perseguir
                this.entity.setAware(true);
                this.currentState = BossState.CHASE;
            }
        }, 30L); // 30 ticks = 1.5 segundos
    }
}
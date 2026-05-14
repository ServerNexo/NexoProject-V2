package me.aeroxis.dungeons.mobs;

import me.aeroxis.core.bosses.AeroxisGlobalBoss;
import me.aeroxis.core.bosses.fsm.BossState;
import me.aeroxis.core.crossplay.CrossplayUtils;
import net.kyori.adventure.bossbar.BossBar; // 🌟 NUEVO IMPORT
import net.kyori.adventure.text.minimessage.MiniMessage; // 🌟 NUEVO IMPORT
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 💀 El Renacido (AeroxisRevenantBoss) - Encuentro Nivel Raid (AAA)
 * Inteligencia: Aggro por DPS, Raycasting, Saltos Parabólicos, Cleave y BossBar.
 */
public class AeroxisRevenantBoss extends AeroxisGlobalBoss {

    private final Random random = new Random();
    private boolean isEnraged = false;

    // Cooldowns independientes para no solapar habilidades
    private int leapCooldown = 0;
    private int beamCooldown = 0;
    private int aggroCheckTimer = 0;

    // 🌟 NUEVO: La BossBar de Kyori Adventure
    private BossBar bossBar;

    public AeroxisRevenantBoss(JavaPlugin plugin, CrossplayUtils crossplayUtils) {
        super(plugin, crossplayUtils, "El Renacido");
    }

    @Override
    protected Mob spawnPhysicalEntity(Location location) {
        Mob boss = (Mob) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);

        boss.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
        boss.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
        boss.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));

        boss.customName(LegacyComponentSerializer.legacyAmpersand().deserialize("&4&lEl Renacido"));
        boss.setCustomNameVisible(true);
        boss.setRemoveWhenFarAway(false);

        if (boss.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) boss.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        if (boss.getAttribute(Attribute.MOVEMENT_SPEED) != null) boss.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.38);
        if (boss.getAttribute(Attribute.FOLLOW_RANGE) != null) boss.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(64.0);
        if (boss.getAttribute(Attribute.STEP_HEIGHT) != null) boss.getAttribute(Attribute.STEP_HEIGHT).setBaseValue(1.5);

        // 🌟 INICIALIZAR BOSSBAR CON GRADIENTE
        this.bossBar = BossBar.bossBar(
                MiniMessage.miniMessage().deserialize("<bold><gradient:#ff3366:#aa00aa>💀 El Renacido 💀</gradient></bold>"),
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.NOTCHED_20
        );

        return boss;
    }

    // ==========================================
    // 🧠 EL CEREBRO DE LA IA
    // ==========================================

    @Override
    protected void handleSpawnPhase() {
        actualizarBossBar(); // 🌟 Mostrar barra mientras spawnea

        if (ticksAlive < 60) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation().add(0, 1, 0), 15, 0.5, 1, 0.5, 0.05);
            });
        } else {
            this.currentState = BossState.IDLE;
        }
    }

    @Override
    protected void handleChasePhase() {
        actualizarBossBar(); // 🌟 Actualizar vida en tiempo real
        checkEnragePhase();
        gestionarCooldowns();

        if (aggroCheckTimer <= 0) {
            evaluarAmenaza();
            aggroCheckTimer = 60;
        }

        if (currentTarget == null) {
            this.currentState = BossState.IDLE;
            return;
        }

        Player target = Bukkit.getPlayer(currentTarget);
        if (target == null || !target.isOnline() || target.isDead() || target.getWorld() != entity.getWorld()) {
            this.currentState = BossState.IDLE;
            return;
        }

        double distance = entity.getLocation().distanceSquared(target.getLocation());

        if (distance <= 6.25) {
            this.currentState = BossState.MELEE_ATTACK;
            return;
        }

        if (distance >= 64.0 && leapCooldown <= 0) {
            castSaltoAbismal(target);
            return;
        }

        if (beamCooldown <= 0 && distance >= 16.0 && distance <= 256.0) {
            if (random.nextInt(100) < 40) {
                castRayoMortal(target);
            }
        }
    }

    @Override
    protected void handleMeleeAttack() {
        actualizarBossBar(); // 🌟
        Location bossLoc = entity.getLocation();
        Vector bossDir = bossLoc.getDirection().normalize();

        bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            bossLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bossLoc.clone().add(bossDir.multiply(1.5)).add(0, 1, 0), 1);
        });

        for (org.bukkit.entity.Entity e : entity.getNearbyEntities(3.0, 2.0, 3.0)) {
            if (e instanceof Player p && !p.isDead()) {
                Vector toPlayer = p.getLocation().toVector().subtract(bossLoc.toVector()).normalize();
                double angle = bossDir.angle(toPlayer);

                if (angle < (Math.PI / 3)) {
                    p.damage(isEnraged ? 15.0 : 10.0, this.entity);
                    if (isEnraged) p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                }
            }
        }

        this.currentState = BossState.CHASE;
    }

    private void gestionarCooldowns() {
        if (leapCooldown > 0) leapCooldown--;
        if (beamCooldown > 0) beamCooldown--;
        if (aggroCheckTimer > 0) aggroCheckTimer--;
    }

    private void evaluarAmenaza() {
        List<Map.Entry<UUID, Double>> topThreats = damageTracker.getTopDamagers(1);
        if (!topThreats.isEmpty()) {
            UUID mayorAmenaza = topThreats.get(0).getKey();

            if (!mayorAmenaza.equals(currentTarget)) {
                Player nuevoObjetivo = Bukkit.getPlayer(mayorAmenaza);
                if (nuevoObjetivo != null && nuevoObjetivo.getWorld() == entity.getWorld()) {
                    this.currentTarget = mayorAmenaza;
                    this.entity.setTarget(nuevoObjetivo);

                    crossplayUtils.sendMessage(nuevoObjetivo, "&#FF3333👁 <bold>El Renacido te ha fijado como su objetivo principal...</bold>");
                    nuevoObjetivo.playSound(nuevoObjetivo.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 1.0f, 0.5f);
                }
            }
        } else {
            super.findTarget();
        }
    }

    // ==========================================
    // 🎭 GESTIÓN DE FASES Y BOSSBAR
    // ==========================================

    // 🌟 NUEVO: Lógica de actualización de la BossBar
    private void actualizarBossBar() {
        if (this.entity == null || this.entity.isDead() || this.bossBar == null) return;

        double health = this.entity.getHealth();
        double maxHealth = this.entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        float progress = (float) (health / maxHealth);

        // Evitamos que crashee si la vida baja de 0 temporalmente
        this.bossBar.progress(Math.max(0.0f, Math.min(1.0f, progress)));

        // Mostrar a todos en el mundo
        for (Player p : this.entity.getWorld().getPlayers()) {
            p.showBossBar(this.bossBar);
        }
    }

    private void checkEnragePhase() {
        if (isEnraged || this.entity == null) return;

        double healthPct = this.entity.getHealth() / this.entity.getAttribute(Attribute.MAX_HEALTH).getValue();

        if (healthPct <= 0.50) {
            this.isEnraged = true;

            // 🌟 Cambiamos el color de la barra en Fase 2
            this.bossBar.color(BossBar.Color.PURPLE);
            this.bossBar.name(MiniMessage.miniMessage().deserialize("<bold><gradient:#aa00aa:#ff3333>💀 EL RENACIDO (FRENESÍ) 💀</gradient></bold>"));

            castFaseDos();
        }
    }

    private void castFaseDos() {
        this.currentState = BossState.CASTING_SKILL;
        this.entity.setAware(false);
        this.entity.setInvulnerable(true);

        Location loc = entity.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);

        for (Player p : entity.getWorld().getPlayers()) {
            crossplayUtils.sendMessage(p, "&#FF3333💀 <bold>¡LEVÁNTENSE, MIS ESCLAVOS!</bold>");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < 3; i++) {
                Location minionLoc = loc.clone().add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);
                LivingEntity minion = (LivingEntity) loc.getWorld().spawnEntity(minionLoc, EntityType.WITHER_SKELETON);
                minion.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 9999, 1));
                minion.customName(LegacyComponentSerializer.legacyAmpersand().deserialize("&cEsclavo Renacido"));
                minion.setCustomNameVisible(true);
            }

            this.entity.setInvulnerable(false);
            this.entity.setAware(true);
            this.currentState = BossState.CHASE;

            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
                if (entity == null || entity.isDead()) { task.cancel(); return; }
                entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 3, 0.4, 0.8, 0.4, 0.02);
            }, 0L, 5L);

        }, 60L);
    }

    // ==========================================
    // 🔮 HABILIDADES DE DESTRUCCIÓN MASSIVA
    // ==========================================

    private void castSaltoAbismal(Player target) {
        this.currentState = BossState.CASTING_SKILL;
        this.leapCooldown = isEnraged ? 100 : 200;

        Location startLoc = entity.getLocation();
        Location targetLoc = target.getLocation();

        Vector trajectory = targetLoc.toVector().subtract(startLoc.toVector());
        trajectory.multiply(0.12);
        trajectory.setY(1.2);

        entity.setVelocity(trajectory);
        startLoc.getWorld().playSound(startLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.5f);

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (entity.isDead()) { task.cancel(); return; }

            if (entity.getVelocity().getY() < -0.1 && entity.isOnGround()) {
                Location landLoc = entity.getLocation();
                landLoc.getWorld().playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                landLoc.getWorld().spawnParticle(Particle.EXPLOSION, landLoc, 3);

                for (org.bukkit.entity.Entity e : entity.getNearbyEntities(4, 2, 4)) {
                    if (e instanceof Player p) {
                        p.damage(12.0, entity);
                        p.setVelocity(new Vector(0, 0.6, 0));
                    }
                }

                this.currentState = BossState.CHASE;
                task.cancel();
            }
        }, 10L, 2L);
    }

    private void castRayoMortal(Player target) {
        this.currentState = BossState.CASTING_SKILL;
        this.beamCooldown = isEnraged ? 120 : 200;
        this.entity.setAware(false);

        Location bossLoc = entity.getEyeLocation();
        Vector direction = target.getLocation().toVector().subtract(bossLoc.toVector()).normalize();

        bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (entity.isDead()) return;

            Location fireLoc = entity.getEyeLocation();
            fireLoc.getWorld().playSound(fireLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.2f);

            for (int i = 0; i < 20; i++) {
                Location particleLoc = fireLoc.clone().add(direction.clone().multiply(i));

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    particleLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, particleLoc, 1);
                });

                for (Player p : fireLoc.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(particleLoc) < 2.25) {
                        p.damage(20.0, entity);
                        p.setFireTicks(60);
                    }
                }
            }

            this.entity.setAware(true);
            this.currentState = BossState.CHASE;
        }, 20L);
    }

    @Override
    protected void onDeath() {
        super.onDeath();

        // 🌟 NUEVO: Limpieza de la BossBar al morir
        if (this.bossBar != null) {
            for (Player p : entity.getWorld().getPlayers()) {
                p.hideBossBar(this.bossBar);
            }
        }

        Location loc = entity.getLocation();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 150, 1.5, 2, 1.5, 0.2);
            loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1);
        });
    }

    // ==========================================
    // 🎁 RECOMPENSAS
    // ==========================================
    @Override
    protected void entregarRecompensaTop(Player player, int rank) {
        if (rank == 1) {
            player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, 3));
            crossplayUtils.sendMessage(player, "&#FFD700🥇 ¡Has recibido 3 Estrellas por ser el TOP 1!");
        } else {
            player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, 1));
            crossplayUtils.sendMessage(player, "&#C0C0C0🥈 ¡Has recibido 1 Estrella por quedar en el TOP " + rank + "!");
        }
    }

    @Override
    protected void entregarRecompensaParticipacion(Player player) {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
        crossplayUtils.sendMessage(player, "&#55FF55✨ ¡Has recibido 1 Diamante por tu valentía!");
    }
}
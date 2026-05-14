package me.aeroxis.cosmetics.listeners;

import com.google.inject.Inject;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.cosmetics.manager.CosmeticManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class CosmeticListener implements Listener {

    private final JavaPlugin plugin;
    private final CosmeticManager cosmeticManager;
    private final CrossplayUtils crossplayUtils;
    private final NamespacedKey bossKey;

    @Inject
    public CosmeticListener(JavaPlugin plugin, CosmeticManager cosmeticManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.cosmeticManager = cosmeticManager;
        this.crossplayUtils = crossplayUtils;
        // 🌟 DNI del Jefe: Si AeroxisCore lo spawneó, tendrá esto pegado en la frente.
        this.bossKey = new NamespacedKey(plugin.getServer().getPluginManager().getPlugin("AeroxisCore"), "boss_id");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        var cosmetics = cosmeticManager.getActiveCosmetics(p.getUniqueId());

        if (cosmetics != null && "DIOS_DEL_RAYO".equals(cosmetics.joinTag()) && p.getWorld().getName().equals("world_hub")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(online.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
            }
            crossplayUtils.broadcastMessage("&#FFDD00⚡ <bold>¡El Dios del Rayo " + p.getName() + " ha descendido al Hub!</bold>");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cosmeticManager.cleanUp(event.getPlayer());
    }

    // ==========================================
    // 🏹 5 ESTELAS DE FLECHAS DINÁMICAS
    // ==========================================
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        var cosmetics = cosmeticManager.getActiveCosmetics(p.getUniqueId());
        if (cosmetics == null || cosmetics.projectileTrail() == null) return;

        // 🌟 SISTEMA DINÁMICO: Elegimos la partícula correcta
        Particle trailParticle = switch (cosmetics.projectileTrail()) {
            case "trail_heart" -> Particle.HEART; // Corazones
            case "trail_magic" -> Particle.TRIAL_OMEN; //
            case "trail_fire" -> Particle.FLAME; // Fuego ardiente
            case "trail_smoke" -> Particle.SQUID_INK; // Humo ninja (tinta negra)
            case "trail_music" -> Particle.NOTE; // Notas musicales
            default -> Particle.CRIT;
        };

        Projectile projectile = (Projectile) event.getProjectile();

        // 🌟 FIX FOLIA: EntityScheduler para el proyectil
        projectile.getScheduler().runAtFixedRate(plugin, task -> {
            if (projectile.isDead() || projectile.isOnGround()) {
                task.cancel();
                return;
            }
            projectile.getWorld().spawnParticle(trailParticle, projectile.getLocation(), 1, 0, 0, 0, 0.0);
        }, null, 1L, 1L);
    }

    // ==========================================
    // 💀 4 FATALITY EFFECTS MASIVOS
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBossKill(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity boss)) return;
        if (!(event.getDamager() instanceof Player killer)) return;

        // 🌟 LECTURA DE DNI: ¿Es un jefe oficial creado por AeroxisCore?
        if (!boss.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING)) return;

        // Si el golpe le va a quitar toda la vida restante
        if ((boss.getHealth() - event.getFinalDamage()) <= 0) {

            var cosmetics = cosmeticManager.getActiveCosmetics(killer.getUniqueId());
            if (cosmetics == null || cosmetics.killEffect() == null) return;

            event.setCancelled(true); // Evitamos que caiga al suelo rojo y muerto como en Vanilla
            Location bossLoc = boss.getLocation();

            // Lo borramos de la existencia instantáneamente (Folia safe)
            boss.getScheduler().execute(plugin, () -> {
                boss.setHealth(0);
                boss.remove();
            }, null, 1L);

            // 🌟 SISTEMA DINÁMICO: 4 Tipos de Ejecución
            switch (cosmetics.killEffect()) {
                case "kill_abduction" -> {
                    bossLoc.getWorld().playSound(bossLoc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
                    Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                        for (int y = 0; y < 15; y++) {
                            bossLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, bossLoc.clone().add(0, y, 0), 10, 0.5, 0.5, 0.5, 0.0);
                        }
                    });
                }
                case "kill_thunder" -> {
                    bossLoc.getWorld().strikeLightningEffect(bossLoc);
                }
                case "kill_hemorrhage" -> {
                    bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                    bossLoc.getWorld().playSound(bossLoc, Sound.BLOCK_HONEY_BLOCK_BREAK, 1.0f, 0.5f); // Sonido visceral
                    Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                        // Explosión roja masiva de bloques
                        bossLoc.getWorld().spawnParticle(Particle.BLOCK, bossLoc.clone().add(0, 1, 0), 150, 1.0, 1.5, 1.0, 0.1, Bukkit.createBlockData(Material.REDSTONE_BLOCK));
                    });
                }
                case "kill_blackhole" -> {
                    bossLoc.getWorld().playSound(bossLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.5f);
                    Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                        bossLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, bossLoc.clone().add(0, 1, 0), 1);
                        bossLoc.getWorld().spawnParticle(Particle.PORTAL, bossLoc.clone().add(0, 1, 0), 200, 2.0, 2.0, 2.0, 1.0);
                    });
                }
            }
        }
    }
}
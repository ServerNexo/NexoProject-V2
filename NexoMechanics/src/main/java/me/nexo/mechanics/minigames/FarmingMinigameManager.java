package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class FarmingMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Integer> plagasActivas = new ConcurrentHashMap<>();

    @Inject
    public FarmingMinigameManager(NexoMechanics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alCosechar(BlockBreakEvent event) {
        Player p = event.getPlayer();

        NexoAPI.getServices().get(ClaimManager.class).ifPresent(claimManager -> {
            me.nexo.protections.core.ProtectionStone stone = claimManager.getStoneAt(event.getBlock().getLocation());
            if (stone != null && !stone.hasPermission(p.getUniqueId(), me.nexo.protections.core.ClaimAction.BREAK)) {
                event.setCancelled(true);
            }
        });

        if (event.isCancelled()) return;

        if (event.getBlock().getBlockData() instanceof Ageable cultivo) {
            if (cultivo.getAge() == cultivo.getMaximumAge() && Math.random() <= 0.01) {
                invocarPlagaMutante(p, event.getBlock().getLocation().add(0.5, 0, 0.5));
            }
        }
    }

    private void invocarPlagaMutante(Player p, org.bukkit.Location loc) {
        ArmorStand plaga = p.getWorld().spawn(loc, ArmorStand.class);
        plaga.setInvisible(true);
        plaga.setSmall(true);

        if (plaga.getEquipment() != null) {
            plaga.getEquipment().setHelmet(new ItemStack(Material.WEEPING_VINES));
        }

        plaga.customName(CrossplayUtils.parseCrossplay(p, configManager.getMessages().mensajes().minijuegos().agricultura().plagaBiologica()));
        plaga.setCustomNameVisible(true);

        plagasActivas.put(plaga.getUniqueId(), 0);
        p.playSound(loc, Sound.ENTITY_SILVERFISH_AMBIENT, 1f, 0.5f);

        CrossplayUtils.sendTitle(p,
                configManager.getMessages().mensajes().minijuegos().agricultura().anomaliaBiologicaTitulo(),
                configManager.getMessages().mensajes().minijuegos().agricultura().anomaliaBiologicaSubtitulo()
        );

        new BukkitRunnable() {
            int tiempoVida = 20;
            @Override
            public void run() {
                if (tiempoVida <= 0 || plaga.isDead() || !plagasActivas.containsKey(plaga.getUniqueId())) {
                    if (!plaga.isDead()) {
                        plaga.getWorld().spawnParticle(Particle.SMOKE, plaga.getLocation(), 10);
                        plaga.remove();
                        plagasActivas.remove(plaga.getUniqueId());
                    }
                    cancel();
                    return;
                }
                Vector salto = new Vector((Math.random() - 0.5) * 1.5, 0.6, (Math.random() - 0.5) * 1.5);
                plaga.setVelocity(salto);
                plaga.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, plaga.getLocation(), 5);
                tiempoVida--;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alGolpearPlaga(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand plaga && plagasActivas.containsKey(plaga.getUniqueId())) {
            event.setCancelled(true);

            if (event.getDamager() instanceof Player p) {
                int golpes = plagasActivas.get(plaga.getUniqueId()) + 1;

                p.playSound(plaga.getLocation(), Sound.ENTITY_SLIME_HURT, 1f, 1f + (golpes * 0.2f));
                plaga.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, plaga.getLocation().add(0, 1, 0), 3);

                if (golpes >= 5) {
                    plagasActivas.remove(plaga.getUniqueId());
                    plaga.remove();

                    p.playSound(plaga.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 2f);
                    plaga.getWorld().spawnParticle(Particle.EXPLOSION, plaga.getLocation(), 1);

                    plaga.getWorld().dropItemNaturally(plaga.getLocation(), new ItemStack(Material.PITCHER_POD, 3));

                    CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().agricultura().amenazaErradicada());
                } else {
                    plagasActivas.put(plaga.getUniqueId(), golpes);
                }
            }
        }
    }
}
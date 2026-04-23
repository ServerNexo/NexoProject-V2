package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
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
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🌾 NexoMechanics - Minijuego de Agricultura RPG (Arquitectura Enterprise)
 * Rendimiento: Folia-Ready EntityScheduler, Cero Estáticos y Dependencias Seguras.
 */
@Singleton
public class FarmingMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    
    // 🌟 Sinergias propagadas estrictamente por Guice
    private final ClaimManager claimManager;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, Integer> plagasActivas = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public FarmingMinigameManager(NexoMechanics plugin, ConfigManager configManager,
                                  ClaimManager claimManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.claimManager = claimManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alCosechar(BlockBreakEvent event) {
        var p = event.getPlayer();

        // 🌟 Verificación Inyectada O(1) sin Service Locators
        if (claimManager != null) {
            var stone = claimManager.getStoneAt(event.getBlock().getLocation());
            if (stone != null && !stone.hasPermission(p.getUniqueId(), me.nexo.protections.core.ClaimAction.BREAK)) {
                event.setCancelled(true);
            }
        }

        if (event.isCancelled()) return;

        if (event.getBlock().getBlockData() instanceof Ageable cultivo) {
            if (cultivo.getAge() == cultivo.getMaximumAge() && Math.random() <= 0.01) {
                invocarPlagaMutante(p, event.getBlock().getLocation().add(0.5, 0, 0.5));
            }
        }
    }

    private void invocarPlagaMutante(Player p, org.bukkit.Location loc) {
        var plaga = p.getWorld().spawn(loc, ArmorStand.class);
        plaga.setInvisible(true);
        plaga.setSmall(true);

        if (plaga.getEquipment() != null) {
            plaga.getEquipment().setHelmet(new ItemStack(Material.WEEPING_VINES));
        }

        // 🌟 Uso de la utilidad inyectada en lugar de estática
        plaga.customName(crossplayUtils.parseCrossplay(p, configManager.getMessages().mensajes().minijuegos().agricultura().plagaBiologica()));
        plaga.setCustomNameVisible(true);

        plagasActivas.put(plaga.getUniqueId(), 0);
        p.playSound(loc, Sound.ENTITY_SILVERFISH_AMBIENT, 1f, 0.5f);

        crossplayUtils.sendTitle(p,
                configManager.getMessages().mensajes().minijuegos().agricultura().anomaliaBiologicaTitulo(),
                configManager.getMessages().mensajes().minijuegos().agricultura().anomaliaBiologicaSubtitulo()
        );

        // 🌟 FOLIA NATIVE: EntityScheduler reemplaza a BukkitRunnable
        // Esto ata el loop lógico estrictamente al hilo del Chunk donde existe la plaga
        var tiempoVida = new AtomicInteger(20);
        
        plaga.getScheduler().runAtFixedRate(plugin, task -> {
            if (tiempoVida.get() <= 0 || plaga.isDead() || !plagasActivas.containsKey(plaga.getUniqueId())) {
                if (!plaga.isDead()) {
                    plaga.getWorld().spawnParticle(Particle.SMOKE, plaga.getLocation(), 10);
                    plaga.remove();
                    plagasActivas.remove(plaga.getUniqueId());
                }
                task.cancel();
                return;
            }
            
            var salto = new Vector((Math.random() - 0.5) * 1.5, 0.6, (Math.random() - 0.5) * 1.5);
            plaga.setVelocity(salto);
            plaga.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, plaga.getLocation(), 5);
            
            tiempoVida.decrementAndGet();
        }, null, 1L, 10L);
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

                    crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().agricultura().amenazaErradicada());
                } else {
                    plagasActivas.put(plaga.getUniqueId(), golpes);
                }
            }
        }
    }
}
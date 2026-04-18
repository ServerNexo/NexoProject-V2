package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class EnchantingMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;

    private final Map<UUID, List<ArmorStand>> runasActivas = new ConcurrentHashMap<>();
    public final Set<UUID> encantamientosGratis = ConcurrentHashMap.newKeySet();

    @Inject
    public EnchantingMinigameManager(NexoMechanics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void alAbrirMesa(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            Player p = (Player) event.getPlayer();
            if (!runasActivas.containsKey(p.getUniqueId()) && Math.random() <= 0.15) {
                invocarRunas(p);
            }
        }
    }

    private void invocarRunas(Player p) {
        Location centro = p.getLocation().add(0, 1.5, 0);
        List<ArmorStand> runas = new ArrayList<>();
        net.kyori.adventure.text.Component[] simbolos = {
                NexoColor.parse("&#00f5ff<bold>🔵</bold>"),
                NexoColor.parse("&#8b0000<bold>🔴</bold>"),
                NexoColor.parse("&#00f5ff<bold>🟢</bold>")
        };

        for (int i = 0; i < 3; i++) {
            ArmorStand runa = p.getWorld().spawn(centro, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setGravity(false);
                as.setSmall(true);
                as.customName(simbolos[runas.size()]);
                as.setCustomNameVisible(true);
                as.setMarker(true);
            });
            runas.add(runa);
        }

        runasActivas.put(p.getUniqueId(), runas);
        p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2f);

        CrossplayUtils.sendTitle(p,
                configManager.getMessages().mensajes().minijuegos().encantamiento().puzzleTitulo(),
                configManager.getMessages().mensajes().minijuegos().encantamiento().puzzleSubtitulo()
        );

        new BukkitRunnable() {
            double angulo = 0;
            int tiempo = 100;
            @Override
            public void run() {
                if (tiempo <= 0 || !runasActivas.containsKey(p.getUniqueId()) || !p.isOnline()) {
                    limpiarRunas(p.getUniqueId());
                    cancel();
                    return;
                }
                for (int i = 0; i < runas.size(); i++) {
                    ArmorStand runa = runas.get(i);
                    if (runa.isDead()) continue;
                    double offset = angulo + (i * (Math.PI * 2 / 3));
                    double x = Math.cos(offset) * 1.5;
                    double z = Math.sin(offset) * 1.5;
                    runa.teleport(p.getLocation().add(x, 1.5, z));
                }
                angulo += 0.1;
                tiempo--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void alGolpearRuna(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand runa && event.getDamager() instanceof Player p) {
            if (!runasActivas.containsKey(p.getUniqueId())) return;
            List<ArmorStand> runas = runasActivas.get(p.getUniqueId());
            if (!runas.contains(runa)) return;
            event.setCancelled(true);

            if (runas.get(0).equals(runa)) {
                p.playSound(runa.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
                runa.getWorld().spawnParticle(Particle.ENCHANT, runa.getLocation().add(0, 0.5, 0), 10);
                runa.remove();
                runas.remove(0);

                if (runas.isEmpty()) {
                    encantamientosGratis.add(p.getUniqueId());
                    runasActivas.remove(p.getUniqueId());
                    CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().encantamiento().sistemaHackeado());
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                }
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
                CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().encantamiento().secuenciaIncorrecta());
                limpiarRunas(p.getUniqueId());
            }
        }
    }

    @EventHandler
    public void alEncantar(EnchantItemEvent event) {
        Player p = event.getEnchanter();
        if (encantamientosGratis.contains(p.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.setLevel(p.getLevel() + event.getExpLevelCost());
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }, 1L);
            encantamientosGratis.remove(p.getUniqueId());
        }
    }

    private void limpiarRunas(UUID id) {
        if (runasActivas.containsKey(id)) {
            for (ArmorStand as : runasActivas.get(id)) {
                if (as != null && !as.isDead()) {
                    as.getWorld().spawnParticle(Particle.SMOKE, as.getLocation(), 5);
                    as.remove();
                }
            }
            runasActivas.remove(id);
        }
    }
}
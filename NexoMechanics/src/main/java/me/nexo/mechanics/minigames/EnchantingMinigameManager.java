package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔮 NexoMechanics - Minijuego de Encantamientos (Arquitectura Enterprise)
 * Rendimiento: Folia-Ready PlayerScheduler, Dependencias Inyectadas y Mutabilidad Segura.
 */
@Singleton
public class EnchantingMinigameManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    private final Map<UUID, List<ArmorStand>> runasActivas = new ConcurrentHashMap<>();
    public final Set<UUID> encantamientosGratis = ConcurrentHashMap.newKeySet();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public EnchantingMinigameManager(NexoMechanics plugin, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler
    public void alAbrirMesa(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            var p = (Player) event.getPlayer();
            if (!runasActivas.containsKey(p.getUniqueId()) && Math.random() <= 0.15) {
                invocarRunas(p);
            }
        }
    }

    private void invocarRunas(Player p) {
        var centro = p.getLocation().add(0, 1.5, 0);
        List<ArmorStand> runas = new ArrayList<>();
        
        // 🌟 Reemplazo de NexoColor viejo por parseo inyectado
        net.kyori.adventure.text.Component[] simbolos = {
                crossplayUtils.parseCrossplay(p, "&#00f5ff<bold>🔵</bold>"),
                crossplayUtils.parseCrossplay(p, "&#8b0000<bold>🔴</bold>"),
                crossplayUtils.parseCrossplay(p, "&#00f5ff<bold>🟢</bold>")
        };

        for (int i = 0; i < 3; i++) {
            var runa = p.getWorld().spawn(centro, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setGravity(false);
                as.setSmall(true);
                as.customName(simbolos[runas.size()]);
                as.setCustomNameVisible(true);
                as.setMarker(true); // 🌟 OPTIMIZACIÓN: Evita cálculos de colisión con otras entidades
            });
            runas.add(runa);
        }

        runasActivas.put(p.getUniqueId(), runas);
        p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2f);

        crossplayUtils.sendTitle(p,
                configManager.getMessages().mensajes().minijuegos().encantamiento().puzzleTitulo(),
                configManager.getMessages().mensajes().minijuegos().encantamiento().puzzleSubtitulo()
        );

        // 🌟 Mutabilidad Segura para el Lambda
        double[] angulo = {0};
        var tiempo = new AtomicInteger(100);

        // 🌟 FOLIA NATIVE: Scheduler atado específicamente al hilo del Jugador
        p.getScheduler().runAtFixedRate(plugin, task -> {
            if (tiempo.get() <= 0 || !runasActivas.containsKey(p.getUniqueId()) || !p.isOnline()) {
                limpiarRunas(p.getUniqueId());
                task.cancel();
                return;
            }
            for (int i = 0; i < runas.size(); i++) {
                var runa = runas.get(i);
                if (runa.isDead()) continue;
                double offset = angulo[0] + (i * (Math.PI * 2 / 3));
                double x = Math.cos(offset) * 1.5;
                double z = Math.sin(offset) * 1.5;
                runa.teleport(p.getLocation().add(x, 1.5, z));
            }
            angulo[0] += 0.1;
            tiempo.decrementAndGet();
        }, null, 1L, 1L); // Empieza en 1 tick, repite cada 1 tick
    }

    @EventHandler
    public void alGolpearRuna(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand runa && event.getDamager() instanceof Player p) {
            if (!runasActivas.containsKey(p.getUniqueId())) return;
            var runas = runasActivas.get(p.getUniqueId());
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
                    crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().encantamiento().sistemaHackeado());
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                }
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
                crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().encantamiento().secuenciaIncorrecta());
                limpiarRunas(p.getUniqueId());
            }
        }
    }

    @EventHandler
    public void alEncantar(EnchantItemEvent event) {
        var p = event.getEnchanter();
        if (encantamientosGratis.contains(p.getUniqueId())) {
            // 🌟 FOLIA NATIVE: Ejecución diferida atada al jugador
            p.getScheduler().runDelayed(plugin, task -> {
                p.setLevel(p.getLevel() + event.getExpLevelCost());
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }, null, 1L);
            encantamientosGratis.remove(p.getUniqueId());
        }
    }

    private void limpiarRunas(UUID id) {
        if (runasActivas.containsKey(id)) {
            for (var as : runasActivas.get(id)) {
                if (as != null && !as.isDead()) {
                    as.getWorld().spawnParticle(Particle.SMOKE, as.getLocation(), 5);
                    as.remove();
                }
            }
            runasActivas.remove(id);
        }
    }
}
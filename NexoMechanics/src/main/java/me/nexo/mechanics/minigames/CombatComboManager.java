package me.nexo.mechanics.minigames;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚔️ NexoMechanics - Minijuego de Combos y Frenesí (Arquitectura Enterprise)
 * Rendimiento: Folia-Ready Schedulers, Mapas Concurrentes O(1) y Cero Estáticos.
 */
@Singleton
public class CombatComboManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    private final Map<UUID, Integer> combos = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ultimoKill = new ConcurrentHashMap<>();
    
    // Mantenido como public para que otras mecánicas puedan leer si está en Frenesí
    public final Map<UUID, Long> enFrenesi = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public CombatComboManager(NexoMechanics plugin, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        
        iniciarDecadenciaCombos();
    }

    @EventHandler
    public void alMatar(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            var p = event.getEntity().getKiller();
            var id = p.getUniqueId();
            long ahora = System.currentTimeMillis();

            int comboActual = combos.getOrDefault(id, 0) + 1;
            combos.put(id, comboActual);
            ultimoKill.put(id, ahora);

            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f + (comboActual * 0.1f));
            crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().combate().rachaCombate().replace("%combo%", String.valueOf(comboActual)));

            if (comboActual == 10) {
                activarFrenesi(p);
                combos.remove(id);
            }
        }
    }

    private void activarFrenesi(Player p) {
        var id = p.getUniqueId();
        enFrenesi.put(id, System.currentTimeMillis() + 10000L);

        crossplayUtils.sendTitle(p,
                configManager.getMessages().mensajes().minijuegos().combate().frenesiTitulo(),
                configManager.getMessages().mensajes().minijuegos().combate().frenesiSubtitulo()
        );
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, false, true));

        var bordeRojo = Bukkit.createWorldBorder();
        bordeRojo.setCenter(p.getLocation());
        bordeRojo.setSize(20000000);
        bordeRojo.setWarningDistance(20000000);
        p.setWorldBorder(bordeRojo);

        // 🌟 FOLIA NATIVE: Scheduler diferido atado al jugador. 
        // Seguro ante teletransportes y desconexiones.
        p.getScheduler().runDelayed(plugin, task -> {
            if (p.isOnline()) {
                p.setWorldBorder(null);
                crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().combate().frenesiDesvanecido());
                enFrenesi.remove(id);
            }
        }, null, 200L);
    }

    private void iniciarDecadenciaCombos() {
        // 🌟 FOLIA NATIVE: GlobalRegionScheduler reemplaza al obsoleto BukkitRunnable
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            long ahora = System.currentTimeMillis();
            
            // 🌟 FIX: removeIf previene ConcurrentModificationException
            combos.entrySet().removeIf(entry -> {
                var id = entry.getKey();
                if (ahora - ultimoKill.getOrDefault(id, 0L) > 3000) {
                    var p = Bukkit.getPlayer(id);
                    if (p != null) {
                        crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().combate().rachaFinalizada());
                    }
                    return true; // Elimina el combo expirado
                }
                return false;
            });
        }, 10L, 10L); // Retraso 10 ticks, repite cada 10 ticks
    }
}
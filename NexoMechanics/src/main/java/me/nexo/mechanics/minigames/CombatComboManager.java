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

@Singleton
public class CombatComboManager implements Listener {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;

    private final Map<UUID, Integer> combos = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ultimoKill = new ConcurrentHashMap<>();
    public final Map<UUID, Long> enFrenesi = new ConcurrentHashMap<>();

    @Inject
    public CombatComboManager(NexoMechanics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        iniciarDecadenciaCombos();
    }

    @EventHandler
    public void alMatar(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player p = event.getEntity().getKiller();
            UUID id = p.getUniqueId();
            long ahora = System.currentTimeMillis();

            int comboActual = combos.getOrDefault(id, 0) + 1;
            combos.put(id, comboActual);
            ultimoKill.put(id, ahora);

            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f + (comboActual * 0.1f));
            CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().combate().rachaCombate().replace("%combo%", String.valueOf(comboActual)));

            if (comboActual == 10) {
                activarFrenesi(p);
                combos.remove(id);
            }
        }
    }

    private void activarFrenesi(Player p) {
        UUID id = p.getUniqueId();
        enFrenesi.put(id, System.currentTimeMillis() + 10000L);

        CrossplayUtils.sendTitle(p,
                configManager.getMessages().mensajes().minijuegos().combate().frenesiTitulo(),
                configManager.getMessages().mensajes().minijuegos().combate().frenesiSubtitulo()
        );
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, false, true));

        WorldBorder bordeRojo = Bukkit.createWorldBorder();
        bordeRojo.setCenter(p.getLocation());
        bordeRojo.setSize(20000000);
        bordeRojo.setWarningDistance(20000000);
        p.setWorldBorder(bordeRojo);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                p.setWorldBorder(null);
                CrossplayUtils.sendMessage(p, configManager.getMessages().mensajes().minijuegos().combate().frenesiDesvanecido());
                enFrenesi.remove(id);
            }
        }, 200L);
    }

    private void iniciarDecadenciaCombos() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long ahora = System.currentTimeMillis();
            for (UUID id : combos.keySet()) {
                if (ahora - ultimoKill.getOrDefault(id, 0L) > 3000) {
                    combos.remove(id);
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) CrossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().minijuegos().combate().rachaFinalizada());
                }
            }
        }, 10L, 10L);
    }
}
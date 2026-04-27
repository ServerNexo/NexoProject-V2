package me.nexo.chat.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.chat.NexoChatPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

@Singleton
public class NexoDeathListener implements Listener {

    private final NexoChatPlugin plugin;
    private final NexoChatManager chatManager;

    @Inject
    public NexoDeathListener(NexoChatPlugin plugin, NexoChatManager chatManager) {
        this.plugin = plugin;
        this.chatManager = chatManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        EntityDamageEvent damageEvent = player.getLastDamageCause();
        
        String cause = "default";

        // 1. Detectar la causa exacta de la muerte
        if (killer != null) {
            cause = "player_kill";
        } else if (damageEvent != null) {
            switch (damageEvent.getCause()) {
                case FALL -> cause = "fall";
                case LAVA -> cause = "lava";
                case DROWNING -> cause = "drowning";
                case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> cause = "entity_kill";
                case VOID -> cause = "void";
                case FIRE, FIRE_TICK -> cause = "fire";
                case PROJECTILE -> cause = "projectile";
                case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> cause = "explosion";
                default -> cause = "default";
            }
        }

        // 2. Leer el texto del config.yml
        String path = "eventos.muertes." + cause;
        String rawMessage = plugin.getConfig().getString(path, plugin.getConfig().getString("eventos.muertes.default", "<gray>☠ %player% ha muerto.</gray>"));

        // 3. Reemplazar las variables (%player% y %killer%)
        rawMessage = rawMessage.replace("%player%", player.getName());
        
        if (killer != null) {
            rawMessage = rawMessage.replace("%killer%", killer.getName());
        } else if (damageEvent != null && damageEvent.getDamageSource().getCausingEntity() != null) {
            // Si lo mató un Zombie, Skeleton, etc. obtenemos su nombre traducido o custom
            String entityName = damageEvent.getDamageSource().getCausingEntity().getName();
            rawMessage = rawMessage.replace("%killer%", entityName);
        } else {
            rawMessage = rawMessage.replace("%killer%", "Entidad Desconocida");
        }

        // 4. Parsear colores (HEX y Legacy) y sobrescribir el mensaje vainilla
        Component finalMessage = chatManager.parseColors(rawMessage);
        event.deathMessage(finalMessage);
    }
}
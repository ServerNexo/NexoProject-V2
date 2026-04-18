package me.nexo.war.listeners;

import com.google.inject.Inject;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.war.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * ⚔️ NexoWar - Listener de Crossplay (Arquitectura Enterprise)
 */
public class WarCrossplayListener implements Listener {

    private final ConfigManager configManager;

    @Inject
    public WarCrossplayListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInterPlatformCombat(EntityDamageByEntityEvent event) {
        // Solo nos interesa si el combate es Jugador vs Jugador
        if (event.getEntity() instanceof Player victima && event.getDamager() instanceof Player atacante) {

            // Verificamos la plataforma de origen mediante los registros de Floodgate
            boolean isBedrockVictim = FloodgateApi.getInstance().isFloodgatePlayer(victima.getUniqueId());
            boolean isBedrockAttacker = FloodgateApi.getInstance().isFloodgatePlayer(atacante.getUniqueId());

            // Si las plataformas no coinciden, se bloquea el daño
            if (isBedrockVictim != isBedrockAttacker) {
                event.setCancelled(true);

                // Alerta táctica enviada a la ActionBar para no saturar el chat
                CrossplayUtils.sendActionBar(atacante, configManager.getMessages().errores().combatePlataformas());
            }
        }
    }
}
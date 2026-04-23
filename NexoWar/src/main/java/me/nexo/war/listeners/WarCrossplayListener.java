package me.nexo.war.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import me.nexo.war.config.ConfigManager;

/**
 * ⚔️ NexoWar - Listener de Crossplay (Arquitectura Enterprise)
 * Evita el combate cruzado entre plataformas de forma inyectada y segura.
 */
@Singleton // 🌟 FIX CRÍTICO: Previene el registro múltiple del listener
public class WarCrossplayListener implements Listener {

    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia con el Core

    // 💉 PILAR 1: Inyección pura (Cero Statics)
    @Inject
    public WarCrossplayListener(ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInterPlatformCombat(EntityDamageByEntityEvent event) {
        // Solo nos interesa si el combate es Jugador vs Jugador
        if (event.getEntity() instanceof Player victima && event.getDamager() instanceof Player atacante) {

            // Verificamos la plataforma de origen mediante los registros de Floodgate (Llamada externa segura)
            boolean isBedrockVictim = FloodgateApi.getInstance().isFloodgatePlayer(victima.getUniqueId());
            boolean isBedrockAttacker = FloodgateApi.getInstance().isFloodgatePlayer(atacante.getUniqueId());

            // Si las plataformas no coinciden, se bloquea el daño
            if (isBedrockVictim != isBedrockAttacker) {
                event.setCancelled(true);

                // 🌟 FIX: Uso de la utilidad inyectada para el ActionBar
                crossplayUtils.sendActionBar(atacante, configManager.getMessages().errores().combatePlataformas());
            }
        }
    }
}
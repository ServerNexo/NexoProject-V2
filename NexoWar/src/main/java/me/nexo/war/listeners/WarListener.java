package me.nexo.war.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.war.config.ConfigManager;
import me.nexo.war.core.WarContract;
import me.nexo.war.managers.WarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/**
 * ⚔️ NexoWar - Listener Principal de Combate (Arquitectura Enterprise)
 * Cero static API calls. Inyectado con Guice y Singleton. Manejo funcional de Optionals.
 */
@Singleton
public class WarListener implements Listener {

    private final WarManager warManager;
    private final UserManager userManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public WarListener(WarManager warManager, UserManager userManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.warManager = warManager;
        this.userManager = userManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🛡️ CONTROL DE CUPOS CROSS-PLAY
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWarCombat(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victima && event.getDamager() instanceof Player atacante) {

            NexoUser uVictima = userManager.getUserOrNull(victima.getUniqueId());
            NexoUser uAtacante = userManager.getUserOrNull(atacante.getUniqueId());

            if (uVictima == null || !uVictima.hasClan() || uAtacante == null || !uAtacante.hasClan()) return;

            UUID clanVictima = uVictima.getClanId();
            UUID clanAtacante = uAtacante.getClanId();

            if (clanVictima.equals(clanAtacante)) return; // Fuego amigo anulado

            // 🌟 FIX: Cadena funcional para Optional. Evita el uso de .get() y los errores del IDE.
            warManager.getGuerraEntre(clanAtacante, clanVictima)
                    .filter(guerra -> guerra.status() == WarContract.WarStatus.ACTIVE)
                    .ifPresent(guerra -> {

                        boolean atacanteIsBedrock = FloodgateApi.getInstance().isFloodgatePlayer(atacante.getUniqueId());
                        boolean isAtacanteClan = clanAtacante.equals(guerra.clanAtacante());

                        // Validamos el Matchmaking y los Cupos
                        if (!guerra.registrarParticipante(atacante.getUniqueId(), isAtacanteClan, atacanteIsBedrock)) {
                            event.setCancelled(true);

                            // Empuje cinético (Lo rebota hacia atrás)
                            Vector pushback = atacante.getLocation().getDirection().multiply(-1).setY(0.3);
                            atacante.setVelocity(pushback);

                            String plataforma = atacanteIsBedrock ? "Táctil/Consola" : "PC (Java)";
                            String msg = configManager.getMessages().errores().escuadronLleno().replace("%plataforma%", plataforma);

                            crossplayUtils.sendActionBar(atacante, msg);
                        }
                    });
        }
    }

    // ==========================================
    // 💀 RASTREO DE BAJAS
    // ==========================================
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victima = event.getEntity();
        Player asesino = victima.getKiller();

        if (asesino == null) return;

        NexoUser uVictima = userManager.getUserOrNull(victima.getUniqueId());
        NexoUser uAsesino = userManager.getUserOrNull(asesino.getUniqueId());

        if (uVictima == null || !uVictima.hasClan()) return;
        if (uAsesino == null || !uAsesino.hasClan()) return;

        UUID clanVictima = uVictima.getClanId();
        UUID clanAsesino = uAsesino.getClanId();

        // 🌟 FIX: Cadena funcional limpia y directa
        warManager.getGuerraEntre(clanAsesino, clanVictima)
                .filter(guerra -> guerra.status() == WarContract.WarStatus.ACTIVE)
                .ifPresent(guerra -> warManager.registrarBaja(guerra, clanAsesino, asesino, victima));
    }
}
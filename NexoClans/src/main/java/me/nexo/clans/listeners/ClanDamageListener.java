package me.nexo.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.core.ClanManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 👥 NexoClans - Motor de Fuego Aliado (Arquitectura Enterprise)
 * Rendimiento: Búsqueda O(1) con Inyección Directa, Pattern Matching Java 21 y omisión de eventos cancelados.
 */
@Singleton
public class ClanDamageListener implements Listener {

    private final ClanManager clanManager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia Inyectada

    // 🎨 PALETA HEX - CONSTANTE DE ALERTA CERO I/O
    private static final String MSG_FRIENDLY_FIRE = "&#ff4b2b[!] Sistema de Protección: No puedes herir a un operario aliado.";

    // 💉 PILAR 1: Inyección Directa de Managers (Cero saltos estáticos hacia NexoCore)
    @Inject
    public ClanDamageListener(ClanManager clanManager, UserManager userManager, CrossplayUtils crossplayUtils) {
        this.clanManager = clanManager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 OPTIMIZACIÓN: ignoreCancelled = true.
    // Si WorldGuard o una zona segura ya bloqueó el golpe, no gastamos CPU haciendo cálculos de clanes.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        // Descarte rápido: Si la víctima no es jugador, cortamos.
        if (!(event.getEntity() instanceof Player victim)) return;

        // 🚀 FAST-FAIL & MODERNIZACIÓN: Switch Pattern Matching (Java 21)
        Player tempAttacker = switch (event.getDamager()) {
            case Player p -> p;
            case Projectile proj when proj.getShooter() instanceof Player p -> p;
            default -> null;
        };

        if (tempAttacker == null || tempAttacker.equals(victim)) return;

        final Player attacker = tempAttacker;

        // Búsquedas O(1) en la RAM Inyectada
        NexoUser userAttacker = userManager.getUserOrNull(attacker.getUniqueId());
        NexoUser userVictim = userManager.getUserOrNull(victim.getUniqueId());

        if (userAttacker != null && userVictim != null &&
                userAttacker.hasClan() && userVictim.hasClan() &&
                userAttacker.getClanId().equals(userVictim.getClanId())) {

            clanManager.getClanFromCache(userAttacker.getClanId()).ifPresent(clan -> {
                if (!clan.isFriendlyFire()) {
                    // 🛡️ ¡Escudo activado instantáneamente!
                    event.setCancelled(true);

                    // 🌟 FIX: Envío Crossplay directo mediante inyección.
                    crossplayUtils.sendMessage(attacker, MSG_FRIENDLY_FIRE);
                }
            });
        }
    }
}
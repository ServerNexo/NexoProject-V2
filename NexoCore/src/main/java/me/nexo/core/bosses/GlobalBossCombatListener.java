package me.nexo.core.bosses;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils; // 🌟 IMPORTANTE
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * ⚔️ Interceptador de Combate - Registra el daño para los Jefes Globales
 */
@Singleton
public class GlobalBossCombatListener implements Listener {

    private final NexoCore plugin;
    private final CrossplayUtils crossplayUtils; // 🌟 AÑADIDO

    @Inject
    public GlobalBossCombatListener(NexoCore plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils; // 🌟 INYECTADO
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // 1. Buscamos si la entidad golpeada es un NexoGlobalBoss activo
        NexoGlobalBoss boss = NexoGlobalBoss.getActiveBoss(victim.getUniqueId());
        if (boss == null) return;

        // 2. Verificamos que el atacante sea un Jugador (directo o con flecha)
        Player attacker = getAttacker(event.getDamager());
        if (attacker == null) return;

        double damage = event.getFinalDamage();

        // 📊 3. Sumamos el daño al Tracker del Jefe en RAM (Thread-Safe)
        boss.getTracker().addDamage(attacker.getUniqueId(), damage);

        // ✨ 4. Spawneamos el numerito flotante del daño
        // 🌟 FIX: Ahora pasamos crossplayUtils como segundo parámetro
        boolean isCritical = event.isCritical();
        DamageIndicator.spawn(plugin, crossplayUtils, victim.getLocation(), damage, isCritical);
    }

    /**
     * Resuelve quién es el atacante real, incluso si usa proyectiles.
     */
    private Player getAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
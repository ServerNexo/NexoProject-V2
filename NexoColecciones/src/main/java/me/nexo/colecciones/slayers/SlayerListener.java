package me.nexo.colecciones.slayers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.managers.EconomyManager; // 🌟 Sinergia Multi-Módulo
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 📚 NexoColecciones - Escucha de Combate Slayer (Arquitectura Enterprise)
 * Rendimiento: O(1) Prevención de robos, Inyección Pura y Atributos 1.21.5 Modernizados.
 */
@Singleton
public class SlayerListener implements Listener {

    private final NexoColecciones plugin;
    private final SlayerManager slayerManager;
    private final CollectionManager collectionManager;
    private final EconomyManager economyManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa (Adiós NexoCore estático)
    @Inject
    public SlayerListener(NexoColecciones plugin, SlayerManager slayerManager, 
                          CollectionManager collectionManager, EconomyManager economyManager,
                          CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.slayerManager = slayerManager;
        this.collectionManager = collectionManager;
        this.economyManager = economyManager;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🛡️ PARCHE ANTI-ROBOS DE JEFES (Protección O(1))
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().hasMetadata("SlayerBoss")) {
            var bossOwnerUUID = event.getEntity().getMetadata("SlayerBoss").get(0).asString();

            Player damager = null;
            if (event.getDamager() instanceof Player p) {
                damager = p;
            } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                damager = p;
            }

            if (damager != null) {
                // Si el que ataca no es el dueño ni un Admin, bloqueamos el daño
                if (!bossOwnerUUID.equals(damager.getUniqueId().toString()) && !damager.hasPermission("nexoslayer.admin")) {
                    event.setCancelled(true);
                    crossplayUtils.sendMessage(damager, "&#FF3366[!] Herejía: &#E6CCFFEste monstruo fue invocado por otro mortal. Tus golpes no surten efecto.");
                }
            } else {
                // Evitamos que fuego, lava u otros monstruos (ej. golems) maten al jefe y arruinen la cacería
                event.setCancelled(true);
            }
        }
    }

    // ==========================================
    // 🗡️ GESTIÓN DE MUERTES (Invocación y Recompensas)
    // ==========================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        var entity = event.getEntity();
        var killer = entity.getKiller();

        // 🏆 1. SI MUERE EL JEFE SLAYER (Finalización del contrato)
        if (entity.hasMetadata("SlayerBoss")) {
            var bossOwnerUUID = entity.getMetadata("SlayerBoss").get(0).asString();
            var bossOwner = Bukkit.getPlayer(UUID.fromString(bossOwnerUUID));

            if (bossOwner != null) {
                var slayer = slayerManager.getActiveSlayer(bossOwner.getUniqueId());

                if (slayer != null && slayer.isBossSpawned()) {
                    if (slayer.getBossBar() != null) {
                        slayer.getBossBar().removeAll();
                    }

                    // 🌟 FIX: Textos inyectados
                    crossplayUtils.sendMessage(bossOwner, "&#555555--------------------------------");
                    crossplayUtils.sendMessage(bossOwner, "&#FFAA00🏆 <bold>CONTRATO COMPLETADO</bold>");
                    crossplayUtils.sendMessage(bossOwner, "&#E6CCFFHas devuelto a &#FF5555" + slayer.getTemplate().bossName() + " &#E6CCFFal vacío.");

                    // 🌟 FIX: Economía atómica asíncrona inyectada vía constructor (Cero ServiceLocator)
                    int recompensaGemas = slayer.getTemplate().requiredKills() / 10;
                    if (recompensaGemas > 0) {
                        economyManager.updateBalanceAsync(bossOwner.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.GEMS, BigDecimal.valueOf(recompensaGemas), true);
                        crossplayUtils.sendMessage(bossOwner, "&#55FF55+ " + recompensaGemas + " Gemas de Cacería");
                    }
                    crossplayUtils.sendMessage(bossOwner, "&#555555--------------------------------");

                    // Sumamos progreso a la colección de Slayers y removemos el contrato
                    collectionManager.addProgress(bossOwner, slayer.getTemplate().id(), 1);
                    slayerManager.removeActiveSlayer(bossOwner.getUniqueId());
                }
            }
            return;
        }

        // 🩸 2. SI MUERE UN MOB NORMAL (Progreso del Contrato)
        if (killer != null) {
            var slayer = slayerManager.getActiveSlayer(killer.getUniqueId());

            if (slayer != null && !slayer.isBossSpawned()) {
                if (entity.getType().name().equalsIgnoreCase(slayer.getTemplate().targetMob())) {
                    slayer.addKill();

                    // Si ya cumplió las kills requeridas, invocamos al Jefe Supremo
                    if (slayer.getKills() >= slayer.getTemplate().requiredKills()) {
                        slayer.setBossSpawned(true);
                        var loc = entity.getLocation();

                        try {
                            var type = EntityType.valueOf(slayer.getTemplate().bossType());
                            var boss = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

                            boss.customName(crossplayUtils.parseCrossplay(killer, "&#FF0000<bold>" + slayer.getTemplate().bossName() + "</bold>"));
                            boss.setCustomNameVisible(true);

                            // 🌟 FIX API 1.21.5: GENERIC_MAX_HEALTH está obsoleto, ahora es simplemente MAX_HEALTH
                            if (boss.getAttribute(Attribute.MAX_HEALTH) != null) {
                                boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(1000.0);
                                boss.setHealth(1000.0);
                            }

                            // Ponemos la marca para que solo el dueño pueda pegarle
                            boss.setMetadata("SlayerBoss", new FixedMetadataValue(plugin, killer.getUniqueId().toString()));

                            // 🌟 FIX: Envío de títulos sin métodos estáticos
                            crossplayUtils.sendTitle(killer, "&#FF0000🔥 <bold>¡LA BESTIA HA DESPERTADO!</bold>", "&#E6CCFFPrepárate para luchar...");
                            crossplayUtils.sendMessage(killer, "&#FF0000[!] Tu matanza ha invocado a " + slayer.getTemplate().bossName() + "!");

                        } catch (Exception e) {
                            crossplayUtils.sendMessage(killer, "&#FF5555[!] Error de la realidad: La bestia intentó nacer pero su forma es inestable.");
                            plugin.getLogger().severe("❌ Error invocando jefe Slayer: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
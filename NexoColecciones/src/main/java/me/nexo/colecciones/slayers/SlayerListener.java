package me.nexo.colecciones.slayers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoAPI;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
 */
@Singleton
public class SlayerListener implements Listener {

    private final NexoColecciones plugin;
    private final SlayerManager slayerManager;
    private final CollectionManager collectionManager;

    // 💉 PILAR 3: Inyección de Dependencias Directa (Adiós NexoCore)
    @Inject
    public SlayerListener(NexoColecciones plugin, SlayerManager slayerManager, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.slayerManager = slayerManager;
        this.collectionManager = collectionManager;
    }

    // ==========================================
    // 🛡️ PARCHE ANTI-ROBOS DE JEFES (Protección O(1))
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().hasMetadata("SlayerBoss")) {
            String bossOwnerUUID = event.getEntity().getMetadata("SlayerBoss").get(0).asString();

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
                    CrossplayUtils.sendMessage(damager, "&#FF3366[!] Herejía: &#E6CCFFEste monstruo fue invocado por otro mortal. Tus golpes no surten efecto.");
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
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // 🏆 1. SI MUERE EL JEFE SLAYER (Finalización del contrato)
        if (entity.hasMetadata("SlayerBoss")) {
            String bossOwnerUUID = entity.getMetadata("SlayerBoss").get(0).asString();
            Player bossOwner = Bukkit.getPlayer(UUID.fromString(bossOwnerUUID));

            if (bossOwner != null) {
                ActiveSlayer slayer = slayerManager.getActiveSlayer(bossOwner.getUniqueId());

                if (slayer != null && slayer.isBossSpawned()) {
                    if (slayer.getBossBar() != null) {
                        slayer.getBossBar().removeAll();
                    }

                    // 🌟 FIX: Textos Hexadecimales directos (0% Lag I/O)
                    CrossplayUtils.sendMessage(bossOwner, "&#555555--------------------------------");
                    CrossplayUtils.sendMessage(bossOwner, "&#FFAA00🏆 <bold>CONTRATO COMPLETADO</bold>");
                    CrossplayUtils.sendMessage(bossOwner, "&#E6CCFFHas devuelto a &#FF5555" + slayer.getTemplate().bossName() + " &#E6CCFFal vacío.");

                    // 🌟 FIX: Economía atómica asíncrona inyectada vía API (Sin acoplar UserManager)
                    int recompensaGemas = slayer.getTemplate().requiredKills() / 10;
                    if (recompensaGemas > 0) {
                        NexoAPI.getServices().get(EconomyManager.class).ifPresent(eco -> {
                            eco.updateBalanceAsync(bossOwner.getUniqueId(), NexoAccount.AccountType.PLAYER, NexoAccount.Currency.GEMS, BigDecimal.valueOf(recompensaGemas), true);
                        });
                        CrossplayUtils.sendMessage(bossOwner, "&#55FF55+ " + recompensaGemas + " Gemas de Cacería");
                    }
                    CrossplayUtils.sendMessage(bossOwner, "&#555555--------------------------------");

                    // Sumamos progreso a la colección de Slayers y removemos el contrato
                    collectionManager.addProgress(bossOwner, slayer.getTemplate().id(), 1);
                    slayerManager.removeActiveSlayer(bossOwner.getUniqueId());
                }
            }
            return;
        }

        // 🩸 2. SI MUERE UN MOB NORMAL (Progreso del Contrato)
        if (killer != null) {
            ActiveSlayer slayer = slayerManager.getActiveSlayer(killer.getUniqueId());

            if (slayer != null && !slayer.isBossSpawned()) {
                if (entity.getType().name().equalsIgnoreCase(slayer.getTemplate().targetMob())) {
                    slayer.addKill();

                    // Si ya cumplió las kills requeridas, invocamos al Jefe Supremo
                    if (slayer.getKills() >= slayer.getTemplate().requiredKills()) {
                        slayer.setBossSpawned(true);
                        Location loc = entity.getLocation();

                        try {
                            EntityType type = EntityType.valueOf(slayer.getTemplate().bossType());
                            LivingEntity boss = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

                            boss.customName(CrossplayUtils.parseCrossplay(killer, "&#FF0000<bold>" + slayer.getTemplate().bossName() + "</bold>"));
                            boss.setCustomNameVisible(true);

                            // Blindaje de vida
                            if (boss.getAttribute(Attribute.MAX_HEALTH) != null) {
                                boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(1000.0);
                                boss.setHealth(1000.0);
                            }

                            // Ponemos la marca para que solo el dueño pueda pegarle
                            boss.setMetadata("SlayerBoss", new FixedMetadataValue(plugin, killer.getUniqueId().toString()));

                            // 🌟 FIX: Textos sin I/O para no generar lagazos durante el spawn
                            CrossplayUtils.sendTitle(killer, "&#FF0000🔥 <bold>¡LA BESTIA HA DESPERTADO!</bold>", "&#E6CCFFPrepárate para luchar...");
                            CrossplayUtils.sendMessage(killer, "&#FF0000[!] Tu matanza ha invocado a " + slayer.getTemplate().bossName() + "!");

                        } catch (Exception e) {
                            CrossplayUtils.sendMessage(killer, "&#FF5555[!] Error de la realidad: La bestia intentó nacer pero su forma es inestable.");
                            plugin.getLogger().severe("❌ Error invocando jefe Slayer: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
package me.nexo.pvp.pasivas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.aurelium.auraskills.api.skill.Skills;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.UUID;

/**
 * 🏛️ NexoPvP - Listener de Pasivas (Arquitectura Enterprise)
 * Rendimiento: Cero String Allocations (Uso de Tags Nativos), Prevención de Memory Leaks.
 * Progresión: Tiers Estrictos en Niveles 15, 30, 50 y 75.
 */
@Singleton
public class PasivasListener implements Listener {

    private final NexoPvP plugin;
    private final PasivasManager manager;
    private final UserManager userManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public PasivasListener(NexoPvP plugin, PasivasManager manager, UserManager userManager,
                           ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.manager = manager;
        this.userManager = userManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    // =========================================================================
    // 🛡️ LIMPIEZA DE MEMORIA (MEMORY LEAK FIX)
    // =========================================================================
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        manager.ultimoTroncoRoto.remove(id);
        manager.invulnerablesUltimaBatalla.remove(id);
    }

    // =========================================================================
    // 💥 HABILIDADES ACTIVAS TIER 4 (NIVEL 75 - END GAME)
    // =========================================================================
    @EventHandler
    public void onHabilidadActiva(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        UUID id = p.getUniqueId();
        long ahora = System.currentTimeMillis();

        if (item.getType().isAir()) return;

        boolean esClickDerecho = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK);
        boolean esClickIzquierdo = (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK);

        // ⛏️ MINERÍA 75: Fiebre del Oro
        if (esClickDerecho && Tag.ITEMS_PICKAXES.isTagged(item.getType()) && manager.getNivel(p, Skills.MINING) >= 75) {
            if (!manager.cdFiebreOro.containsKey(id) || (ahora - manager.cdFiebreOro.get(id)) > 300_000L) {
                manager.cdFiebreOro.put(id, ahora);
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 300, 2));
                p.getWorld().spawnParticle(Particle.WAX_ON, p.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5);
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
                crossplayUtils.sendActionBar(p, "&#FFAA00⚡ ¡Fiebre del Oro Activada!");
            }
        }
        // 🪓 FORRAJERO 75: Furia del Leñador
        else if (esClickDerecho && Tag.ITEMS_AXES.isTagged(item.getType()) && manager.getNivel(p, Skills.FORAGING) >= 75) {
            if (!manager.cdFuriaLenador.containsKey(id) || (ahora - manager.cdFuriaLenador.get(id)) > 300_000L) {
                manager.cdFuriaLenador.put(id, ahora);
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 1));
                p.getWorld().strikeLightningEffect(p.getLocation());
                crossplayUtils.sendActionBar(p, "&#FF5555🪓 ¡Furia del Leñador Activada!");
            }
        }
        // 🌾 AGRICULTURA 75: Cosecha Divina
        else if (esClickDerecho && Tag.ITEMS_HOES.isTagged(item.getType()) && manager.getNivel(p, Skills.FARMING) >= 75) {
            if (event.getClickedBlock() == null) return;
            if (!manager.cdCosechaDivina.containsKey(id) || (ahora - manager.cdCosechaDivina.get(id)) > 120_000L) {
                manager.cdCosechaDivina.put(id, ahora);
                Block centro = p.getLocation().getBlock();
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        Block b = centro.getRelative(x, 0, z);
                        if (b.getBlockData() instanceof org.bukkit.block.data.Ageable cultivo && cultivo.getAge() < cultivo.getMaximumAge()) {
                            b.applyBoneMeal(BlockFace.UP);
                            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation(), 2);
                        }
                    }
                }
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
                crossplayUtils.sendActionBar(p, "&#55FF55🌾 ¡Cosecha Divina Activada!");
            }
        }
        // 🎣 PESCADERÍA 75: Llamada de Poseidón
        else if (esClickIzquierdo && item.getType() == Material.FISHING_ROD && manager.getNivel(p, Skills.FISHING) >= 75) {
            if (!manager.cdPoseidon.containsKey(id) || (ahora - manager.cdPoseidon.get(id)) > 300_000L) {
                manager.cdPoseidon.put(id, ahora);
                p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 600, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 600, 0));
                p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation().add(0, 1, 0), 100);
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 2f);
                crossplayUtils.sendActionBar(p, "&#00f5ff🌊 ¡Llamada de Poseidón Activada!");
            }
        }
        // ⚔️ LUCHA 75: Golpe Sísmico
        else if (esClickDerecho && p.isSneaking() && Tag.ITEMS_SWORDS.isTagged(item.getType()) && manager.getNivel(p, Skills.FIGHTING) >= 75) {
            if (!manager.cdGolpeSismico.containsKey(id) || (ahora - manager.cdGolpeSismico.get(id)) > 60_000L) {
                manager.cdGolpeSismico.put(id, ahora);
                p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 2);
                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                for (org.bukkit.entity.Entity entidad : p.getNearbyEntities(5, 3, 5)) {
                    if (entidad instanceof org.bukkit.entity.LivingEntity enemigo && enemigo != p) {
                        enemigo.setVelocity(new Vector(0, 1.2, 0));
                        enemigo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                    }
                }
                crossplayUtils.sendActionBar(p, "&#FF5555💥 ¡Golpe Sísmico!");
            }
        }
        // 🧪 ALQUIMIA 75: Transmutación Vital
        else if (esClickDerecho && item.getType() == Material.GLOWSTONE_DUST && manager.getNivel(p, Skills.ALCHEMY) >= 75) {
            if (!manager.cdTransmutacion.containsKey(id) || (ahora - manager.cdTransmutacion.get(id)) > 180_000L) {
                manager.cdTransmutacion.put(id, ahora);
                item.setAmount(item.getAmount() - 1);
                p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
                p.removePotionEffect(PotionEffectType.POISON);
                p.removePotionEffect(PotionEffectType.WITHER);
                p.removePotionEffect(PotionEffectType.SLOWNESS);
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.removePotionEffect(PotionEffectType.DARKNESS);
                p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 2, 0), 5);
                p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1f);
                crossplayUtils.sendActionBar(p, "&#FF55FF✨ ¡Transmutación Vital!");
            }
        }
    }

    // =========================================================================
    // ⚔️ PASIVAS DE COMBATE
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombate(EntityDamageByEntityEvent event) {
        // 🌟 ATACANTE
        if (event.getDamager() instanceof Player atacante) {
            NexoUser user = userManager.getUserOrNull(atacante.getUniqueId());
            int nivel = user != null ? user.getCombateNivel() : 1;

            // Tier 2 (30): Ejecución (+20% daño a enemigos con <20% de HP)
            if (nivel >= 30 && event.getEntity() instanceof org.bukkit.entity.LivingEntity victima) {
                double hpPercent = victima.getHealth() / victima.getAttribute(Attribute.MAX_HEALTH).getValue();
                if (hpPercent <= 0.20) {
                    event.setDamage(event.getDamage() * 1.20);
                }
            }

            // Tier 1 (15): Robo de Vida (Cura el 5% del daño final)
            if (nivel >= 15) {
                double cura = event.getFinalDamage() * 0.05;
                double maxHp = atacante.getAttribute(Attribute.MAX_HEALTH).getValue();
                atacante.setHealth(Math.min(maxHp, atacante.getHealth() + cura));
            }
        }

        // 🌟 VÍCTIMA
        if (event.getEntity() instanceof Player victima) {
            UUID id = victima.getUniqueId();

            if (manager.invulnerablesUltimaBatalla.containsKey(id)) {
                if (System.currentTimeMillis() < manager.invulnerablesUltimaBatalla.get(id)) {
                    event.setCancelled(true);
                    return;
                } else {
                    manager.invulnerablesUltimaBatalla.remove(id);
                }
            }

            NexoUser user = userManager.getUserOrNull(id);
            int nivel = user != null ? user.getCombateNivel() : 1;

            // Tier 3 (50): Última Batalla (Tótem Visual y 3s de inmunidad)
            if (nivel >= 50 && event.getFinalDamage() >= victima.getHealth()) {
                long ahora = System.currentTimeMillis();
                long cooldownMilis = 10 * 60 * 1000L; // 10 Minutos

                if (!manager.cdUltimaBatalla.containsKey(id) || (ahora - manager.cdUltimaBatalla.get(id)) > cooldownMilis) {
                    event.setCancelled(true);
                    victima.setHealth(1.0);

                    manager.invulnerablesUltimaBatalla.put(id, ahora + 3000L);
                    manager.cdUltimaBatalla.put(id, ahora);

                    victima.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, victima.getLocation(), 100);
                    victima.playSound(victima.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);

                    var mainTitle = crossplayUtils.parseCrossplay(victima, configManager.getMessages().mensajes().pvp().escudoEmergenciaTitulo());
                    var subTitle = crossplayUtils.parseCrossplay(victima, configManager.getMessages().mensajes().pvp().escudoEmergenciaSub());
                    var times = Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(2000), Duration.ofMillis(250));
                    victima.showTitle(Title.title(mainTitle, subTitle, times));
                }
            }
        }
    }

    // =========================================================================
    // ⛏️ PASIVAS DE ENTORNO (Minería, Tala, Agricultura)
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDanoGeneral(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        if (manager.invulnerablesUltimaBatalla.containsKey(p.getUniqueId()) && System.currentTimeMillis() < manager.invulnerablesUltimaBatalla.get(p.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Tier 2 (30) Minería: Resistencia al calor
        if (event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            NexoUser user = userManager.getUserOrNull(p.getUniqueId());
            if (user != null && user.getMineriaNivel() >= 30) {
                if (Tag.ITEMS_PICKAXES.isTagged(p.getInventory().getItemInMainHand().getType())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Block b = event.getBlock();
        Material tipo = b.getType();

        NexoUser user = userManager.getUserOrNull(p.getUniqueId());
        if (user == null) return;

        int nivelTala = manager.getNivel(p, Skills.FORAGING);
        int nivelMina = user.getMineriaNivel();
        int nivelGranja = user.getAgriculturaNivel();

        // Tier 1 (15) Tala: Drop de Manzanas
        if (nivelTala >= 15 && Tag.LEAVES.isTagged(tipo)) {
            if (Math.random() <= 0.05) {
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.APPLE));
            }
        }

        // Tier 2 (30) Tala: Postura Inamovible (Antiknockback)
        if (Tag.LOGS.isTagged(tipo)) {
            manager.ultimoTroncoRoto.put(p.getUniqueId(), System.currentTimeMillis());
            if (nivelTala >= 30 && Math.random() <= 0.05) {
                p.playSound(b.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1.5f);
            }
        }

        // Tier 3 (50) Minería: Explosión en Cadena
        if (nivelMina >= 50 && Tag.BASE_STONE_OVERWORLD.isTagged(tipo)) {
            if (Math.random() <= 0.01) {
                b.getWorld().spawnParticle(Particle.EXPLOSION, b.getLocation(), 1);
                b.getWorld().playSound(b.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Block exp = b.getRelative(x, 0, z);
                        if (Tag.BASE_STONE_OVERWORLD.isTagged(exp.getType())) {
                            exp.breakNaturally(p.getInventory().getItemInMainHand());
                        }
                    }
                }
            }
        }

        // Tier 2 (30) Agricultura: Zanahoria Dorada
        if (nivelGranja >= 30 && b.getBlockData() instanceof org.bukkit.block.data.Ageable cultivo) {
            if (cultivo.getAge() == cultivo.getMaximumAge() && Math.random() <= 0.10) {
                b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.GOLDEN_CARROT));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
            }
        }
    }

    // =========================================================================
    // 🧲 OTRAS PASIVAS (Knockback, Cultivos, Pesca, Pociones)
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKnockback(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player p && event.getDamager() instanceof Monster) {
            // Tier 2 (30) Forrajero: Resistencia al Knockback temporal
            if (manager.getNivel(p, Skills.FORAGING) >= 30) {
                Long ultimoTala = manager.ultimoTroncoRoto.get(p.getUniqueId());
                if (ultimoTala != null && (System.currentTimeMillis() - ultimoTala) <= 2000) {
                    Bukkit.getScheduler().runTask(plugin, () -> p.setVelocity(new Vector(0, p.getVelocity().getY(), 0)));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPisadas(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null) {
            NexoUser user = userManager.getUserOrNull(event.getPlayer().getUniqueId());
            // Tier 1 (15) Agricultura: Pies ligeros (No romper cultivos)
            if (event.getClickedBlock().getType() == Material.FARMLAND && user != null && user.getAgriculturaNivel() >= 15) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPescado(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player p = event.getPlayer();
            int nivel = manager.getNivel(p, Skills.FISHING);

            // Tier 1 (15) Pescadería: Recuperación de energía
            if (nivel >= 15) {
                NexoUser user = userManager.getUserOrNull(p.getUniqueId());
                if (user != null) {
                    int maxEnergia = 100 + ((user.getNexoNivel() - 1) * 20) + user.getEnergiaExtraAccesorios();
                    user.setEnergiaMineria(Math.min(user.getEnergiaMineria() + 5, maxEnergia));
                }
            }

            // Tier 2 (30) Pescadería: Doble Drop
            if (nivel >= 30 && event.getCaught() instanceof Item itemEntity && Math.random() <= 0.10) {
                ItemStack caught = itemEntity.getItemStack();
                caught.setAmount(caught.getAmount() * 2);
                itemEntity.setItemStack(caught);
                crossplayUtils.sendActionBar(p, configManager.getMessages().mensajes().pvp().pescaCuantica());
            }
        }
    }

    @EventHandler
    public void onAire(EntityAirChangeEvent event) {
        // Tier 3 (50) Pescadería: Gracia de Delfín
        if (event.getEntity() instanceof Player p && manager.getNivel(p, Skills.FISHING) >= 50) {
            if (event.getAmount() < p.getRemainingAir()) {
                event.setCancelled(true);
                p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 100, 0, false, false, false));
            }
        }
    }

    @EventHandler
    public void onBeber(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.POTION) {
            Player p = event.getPlayer();

            // Tier 2 (30) Alquimia: Buff de duración
            if (manager.getNivel(p, Skills.ALCHEMY) >= 30) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (PotionEffect effect : p.getActivePotionEffects()) {
                        p.addPotionEffect(new PotionEffect(effect.getType(), (int) (effect.getDuration() * 1.2), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
                    }
                });
            }
        }
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        // Tier 1 Alquimia (Global 10%)
        if (Math.random() <= 0.10) {
            for (ItemStack item : event.getContents().getContents()) {
                if (item != null && item.getType() == Material.POTION) {
                    item.setAmount(Math.min(64, item.getAmount() * 2));
                }
            }
        }
    }

    @EventHandler
    public void onXpGain(PlayerExpChangeEvent event) {
        Player p = event.getPlayer();

        // Tier 1 (15) Encantamiento: Boost XP
        if (manager.getNivel(p, Skills.ENCHANTING) >= 15) {
            event.setAmount((int) (event.getAmount() * 1.10));
        }

        // Tier 4 (75) Encantamiento: Aura de Sabiduría (Mending Pasivo)
        if (manager.getNivel(p, Skills.ENCHANTING) >= 75) {
            ItemStack mano = p.getInventory().getItemInMainHand();
            if (mano.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable && damageable.hasDamage()) {
                if (Math.random() <= 0.30) {
                    int reparacion = event.getAmount() * 2;
                    damageable.setDamage(Math.max(0, damageable.getDamage() - reparacion));
                    mano.setItemMeta(damageable);
                    p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1, 0), 10);
                }
            }
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        Player p = event.getEnchanter();
        // Tier 2 (30) Encantamiento: Retención de Energía
        if (manager.getNivel(p, Skills.ENCHANTING) >= 30 && Math.random() <= 0.15) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.setLevel(p.getLevel() + event.getExpLevelCost());
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
                crossplayUtils.sendMessage(p, configManager.getMessages().mensajes().pvp().retencionEnergia());
            });
        }
    }
}
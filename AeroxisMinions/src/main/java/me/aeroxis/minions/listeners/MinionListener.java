package me.aeroxis.minions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.factories.listeners.LogisticsLinkerListener;
import me.aeroxis.islas.data.IslandProfile;
import me.aeroxis.islas.managers.IslandManager;
import me.aeroxis.minions.AeroxisMinions;
import me.aeroxis.minions.config.ConfigManager;
import me.aeroxis.minions.data.MinionDNA;
import me.aeroxis.minions.data.MinionKeys;
import me.aeroxis.minions.data.UpgradesConfig;
import me.aeroxis.minions.manager.ActiveMinion;
import me.aeroxis.minions.manager.MinionManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * 🤖 AeroxisMinions - Listener Principal (Arquitectura Enterprise)
 * Rendimiento: Decodificación Binaria, Validación O(1) y Enrutamiento Logístico.
 */
@Singleton
public class MinionListener implements Listener {

    private final AeroxisMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;
    private final UpgradesConfig upgradesConfig;
    private final CrossplayUtils crossplayUtils;
    private final IslandManager islandManager;

    @Inject
    public MinionListener(AeroxisMinions plugin, MinionManager minionManager, ConfigManager configManager,
                          UpgradesConfig upgradesConfig, CrossplayUtils crossplayUtils, IslandManager islandManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;
        this.upgradesConfig = upgradesConfig;
        this.crossplayUtils = crossplayUtils;
        this.islandManager = islandManager;
    }

    // =========================================
    // 🟩 EVENTO 1: COLOCAR EL MINION
    // =========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onColocarMinion(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        var item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();

        if (meta.getPersistentDataContainer().has(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE)) {
            event.setCancelled(true);
            var player = event.getPlayer();
            Location loc = event.getClickedBlock().getLocation();

            if (loc.getWorld() == null || !loc.getWorld().getName().startsWith("island_")) {
                crossplayUtils.sendMessage(player, "&#FF5555[x] Los Minions solo pueden materializarse dentro de tu Isla.");
                player.playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            IslandProfile profile = islandManager.getIslandAt(loc);

            if (profile == null) {
                crossplayUtils.sendMessage(player, "&#FF5555[x] Error detectando la propiedad de esta isla.");
                return;
            }

            if (!profile.isMember(player.getUniqueId())) {
                crossplayUtils.sendMessage(player, configManager.getMessages().manager().dominioAjeno());
                player.playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            try {
                MinionDNA itemDna = meta.getPersistentDataContainer().get(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE);

                if (itemDna != null) {
                    int maxMinions = profile.getRealMinionLimit();

                    long placedMinions = minionManager.getMinionsActivos().values().stream()
                            .filter(m -> m.getEntity() != null && m.getEntity().isValid())
                            .filter(m -> m.getEntity().getLocation().getWorld().equals(loc.getWorld()))
                            .count();

                    if (placedMinions >= maxMinions) {
                        crossplayUtils.sendMessage(player, configManager.getMessages().manager().limiteAlcanzado().replace("%max%", String.valueOf(maxMinions)));
                        crossplayUtils.sendMessage(player, "&#FFAA00💡 ¡Mejora el Límite de Minions en el menú de la isla!");
                        player.playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }

                    var spawnLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0.5, 0.5);

                    minionManager.spawnMinion(spawnLoc, player.getUniqueId(), itemDna.currentProductionId(), itemDna.tier());

                    item.setAmount(item.getAmount() - 1);

                    String msg = configManager.getMessages().manager().esclavoConjurado()
                            .replace("%type%", itemDna.currentProductionId())
                            .replace("%placed%", String.valueOf(placedMinions + 1))
                            .replace("%max%", String.valueOf(maxMinions));

                    crossplayUtils.sendMessage(player, msg);
                    player.playSound(spawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.5f);
                }
            } catch (Exception e) {
                crossplayUtils.sendMessage(player, configManager.getMessages().manager().selloCorrupto());
            }
        }
    }

    // =========================================
    // 🟥 EVENTO 2: ROMPER BLOQUE BAJO EL MINION
    // =========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var topLoc = event.getBlock().getLocation().add(0.5, 1.0, 0.5);
        var player = event.getPlayer();

        for (Entity entity : topLoc.getWorld().getNearbyEntities(topLoc, 0.5, 0.5, 0.5)) {
            if (entity instanceof Interaction hitbox) {

                String displayIdStr = hitbox.getPersistentDataContainer().get(MinionKeys.INTERACTION_ID, PersistentDataType.STRING);

                if (displayIdStr != null) {
                    try {
                        UUID displayId = UUID.fromString(displayIdStr);
                        ActiveMinion minion = minionManager.getMinion(displayId);

                        if (minion != null) {
                            if (!minion.getDna().ownerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
                                crossplayUtils.sendMessage(player, configManager.getMessages().manager().desestabilizarAjeno());
                                event.setCancelled(true);
                                return;
                            }

                            minionManager.recogerMinion(player, displayId);
                            break;
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    // =========================================
    // 🛡️ PROTECCIÓN DE ÍTEMS ARCANOS (MEJORAS)
    // =========================================

    @EventHandler(priority = EventPriority.LOWEST)
    public void onColocarMejora(BlockPlaceEvent event) {
        var item = event.getItemInHand();
        if (item.getType().isAir()) return;

        if (upgradesConfig.getUpgradeData(item) != null) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(event.getPlayer(), configManager.getMessages().manager().mejoraComoBloque());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDerramarLava(PlayerBucketEmptyEvent event) {
        var item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() != event.getBucket()) {
            item = event.getPlayer().getInventory().getItemInOffHand();
        }

        if (item.getType() != org.bukkit.Material.AIR) {
            if (upgradesConfig.getUpgradeData(item) != null) {
                event.setCancelled(true);
                crossplayUtils.sendMessage(event.getPlayer(), configManager.getMessages().manager().mejoraComoLiquido());
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractuarConMejora(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType().isAir()) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (upgradesConfig.getUpgradeData(event.getItem()) != null) {
                event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            }
        }
    }

    // =========================================
    // 🔗 FASE 4: ESCUCHA DE ENRUTAMIENTO WI-FI
    // =========================================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMinionLinkedToFactory(LogisticsLinkerListener.MinionLinkedEvent event) {
        // Obtenemos el minion activo en la RAM
        ActiveMinion minion = minionManager.getMinion(event.getMinionId());

        if (minion != null) {
            // Actualizamos su destino y lo guardamos atómicamente en su PDC
            minion.setTargetLinkId(event.getFactoryId());
        }
    }
}
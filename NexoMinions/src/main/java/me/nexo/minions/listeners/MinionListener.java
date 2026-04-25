package me.nexo.minions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionDNA;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * 🤖 NexoMinions - Listener Principal (Arquitectura Enterprise)
 * Rendimiento: Decodificación Binaria O(1), Event-Driven Protections y Lógica de Genoma.
 */
@Singleton
public class MinionListener implements Listener {

    private final NexoMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;
    private final UpgradesConfig upgradesConfig;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public MinionListener(NexoMinions plugin, MinionManager minionManager, ConfigManager configManager,
                          UpgradesConfig upgradesConfig, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;
        this.upgradesConfig = upgradesConfig;
        this.crossplayUtils = crossplayUtils;
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

        // 🧬 FASE 3: Verificamos si el ítem tiene ADN Binario
        if (meta.getPersistentDataContainer().has(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE)) {
            event.setCancelled(true);
            var player = event.getPlayer();

            if (!canBuild(player, event.getClickedBlock().getLocation())) {
                crossplayUtils.sendMessage(player, configManager.getMessages().manager().dominioAjeno());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            try {
                // Leemos el ADN que viene en el ítem (normalmente con owner en ceros)
                MinionDNA itemDna = meta.getPersistentDataContainer().get(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE);

                if (itemDna != null) {
                    int maxMinions = minionManager.getMaxMinions(player);
                    int placedMinions = minionManager.getPlacedMinions(player);

                    if (placedMinions >= maxMinions) {
                        crossplayUtils.sendMessage(player, configManager.getMessages().manager().limiteAlcanzado().replace("%max%", String.valueOf(maxMinions)));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }

                    var spawnLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);

                    // 🧬 MUTACIÓN DE COLOCACIÓN: Le asignamos el dueño real al ADN antes de spawnear
                    // Como el dueño en el ítem solía ser 0000-0000..., aquí se vincula permanentemente al jugador.
                    minionManager.spawnMinion(spawnLoc, player.getUniqueId(), itemDna.type(), itemDna.tier());
                    minionManager.addPlacedMinion(player, 1);

                    item.setAmount(item.getAmount() - 1);

                    String msg = configManager.getMessages().manager().esclavoConjurado()
                            .replace("%type%", itemDna.type().getDisplayName())
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
                        var minion = minionManager.getMinion(displayId);

                        if (minion != null) {
                            // 🧬 LECTURA DE ADN: Validamos el dueño desde el genoma
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
    // 🛡️ PROTECCIÓN DE ÍTEMS ARCANOS
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

    private boolean canBuild(Player player, Location loc) {
        if (!Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) return true;

        var fakeEvent = new BlockPlaceEvent(loc.getBlock(), loc.getBlock().getState(), loc.getBlock(), new ItemStack(org.bukkit.Material.DIRT), player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(fakeEvent);

        return !fakeEvent.isCancelled();
    }
}
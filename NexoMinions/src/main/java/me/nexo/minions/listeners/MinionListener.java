package me.nexo.minions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * 🤖 NexoMinions - Listener Principal de Bloques (Arquitectura Enterprise)
 * Rendimiento: Cero Objetos Basura, Desacoplado de NexoProtections.
 */
@Singleton
public class MinionListener implements Listener {

    private final NexoMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;
    private final UpgradesConfig upgradesConfig;

    // 🌟 OPTIMIZACIÓN O(1): Cacheamos la llave para no instanciarla por cada bloque que se rompe en el server.
    private final NamespacedKey interactionKey;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public MinionListener(NexoMinions plugin, MinionManager minionManager, ConfigManager configManager, UpgradesConfig upgradesConfig) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;
        this.upgradesConfig = upgradesConfig;

        this.interactionKey = new NamespacedKey(plugin, "minion_display_id");
    }

    // =========================================
    // 🟩 EVENTO 1: COLOCAR EL MINION
    // =========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onColocarMinion(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        // Verificamos si es un Minion Oficial
        if (meta.getPersistentDataContainer().has(MinionKeys.TYPE, PersistentDataType.STRING)) {
            event.setCancelled(true); // Evitamos que ponga la cabeza/bloque físico en el mundo
            Player player = event.getPlayer();

            // 🌟 FIX DESACOPLADO: Comprobación de Protección sin depender de la API de NexoProtections
            // Esto evita errores de Maven y mantiene a NexoMinions como un microservicio independiente.
            if (!canBuild(player, event.getClickedBlock().getLocation())) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().manager().dominioAjeno());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            try {
                String typeStr = meta.getPersistentDataContainer().get(MinionKeys.TYPE, PersistentDataType.STRING);

                Integer tier = 1; // Valor por defecto
                if (meta.getPersistentDataContainer().has(MinionKeys.TIER, PersistentDataType.INTEGER)) {
                    tier = meta.getPersistentDataContainer().get(MinionKeys.TIER, PersistentDataType.INTEGER);
                }

                if (typeStr != null) {
                    int maxMinions = minionManager.getMaxMinions(player);
                    int placedMinions = minionManager.getPlacedMinions(player);

                    if (placedMinions >= maxMinions) {
                        CrossplayUtils.sendMessage(player, configManager.getMessages().manager().limiteAlcanzado().replace("%max%", String.valueOf(maxMinions)));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }

                    MinionType type = MinionType.valueOf(typeStr);
                    Location spawnLoc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);

                    minionManager.spawnMinion(spawnLoc, player.getUniqueId(), type, tier);
                    minionManager.addPlacedMinion(player, 1);

                    item.setAmount(item.getAmount() - 1);

                    String msg = configManager.getMessages().manager().esclavoConjurado()
                            .replace("%type%", type.getDisplayName())
                            .replace("%placed%", String.valueOf(placedMinions + 1))
                            .replace("%max%", String.valueOf(maxMinions));

                    CrossplayUtils.sendMessage(player, msg);
                    player.playSound(spawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f, 1.5f);
                }
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, configManager.getMessages().manager().selloCorrupto());
            }
        }
    }

    // =========================================
    // 🟥 EVENTO 2: ROMPER BLOQUE BAJO EL MINION
    // =========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location topLoc = event.getBlock().getLocation().add(0.5, 1.0, 0.5);
        Player player = event.getPlayer();

        for (Entity entity : topLoc.getWorld().getNearbyEntities(topLoc, 0.5, 0.5, 0.5)) {
            if (entity instanceof Interaction hitbox) {

                // Usamos la llave en RAM
                String displayIdStr = hitbox.getPersistentDataContainer().get(interactionKey, PersistentDataType.STRING);

                if (displayIdStr != null) {
                    try {
                        ActiveMinion minion = minionManager.getMinion(UUID.fromString(displayIdStr));

                        if (minion != null) {
                            // 🌟 SEGURIDAD ABSOLUTA
                            if (!minion.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
                                CrossplayUtils.sendMessage(player, configManager.getMessages().manager().desestabilizarAjeno());
                                event.setCancelled(true);
                                return;
                            }

                            minionManager.recogerMinion(player, UUID.fromString(displayIdStr));
                            break; // El bloque se romperá normalmente y el minion será recogido
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
        ItemStack item = event.getItemInHand();
        if (item.getType().isAir()) return;

        if (upgradesConfig.getUpgradeData(item) != null) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(event.getPlayer(), configManager.getMessages().manager().mejoraComoBloque());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDerramarLava(PlayerBucketEmptyEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() != event.getBucket()) {
            item = event.getPlayer().getInventory().getItemInOffHand();
        }

        if (item.getType() != org.bukkit.Material.AIR) {
            if (upgradesConfig.getUpgradeData(item) != null) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(event.getPlayer(), configManager.getMessages().manager().mejoraComoLiquido());
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractuarConMejora(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType().isAir()) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (upgradesConfig.getUpgradeData(event.getItem()) != null) {
                event.setUseItemInHand(org.bukkit.event.Event.Result.DENY); // Denegamos el uso interactivo de la mejora (como tirar una bola de nieve)
            }
        }
    }

    // =========================================
    // 🔗 UTILIDAD DE DESACOPLAMIENTO
    // =========================================
    /**
     * Verifica si un jugador puede construir en una zona delegando el trabajo a Bukkit Events
     * en lugar de acoplarse a la API de NexoProtections, o usa un evento de bloque simulado.
     */
    private boolean canBuild(Player player, Location loc) {
        // Si el plugin de protecciones no está, asumimos que es libre
        if (!Bukkit.getPluginManager().isPluginEnabled("NexoProtections")) return true;

        // Simulamos un evento de colocación de bloque. Si NexoProtections u otro plugin (como WorldGuard)
        // lo cancela, significa que el jugador no tiene permisos aquí.
        BlockPlaceEvent fakeEvent = new BlockPlaceEvent(loc.getBlock(), loc.getBlock().getState(), loc.getBlock(), new ItemStack(org.bukkit.Material.DIRT), player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(fakeEvent);

        return !fakeEvent.isCancelled();
    }
}
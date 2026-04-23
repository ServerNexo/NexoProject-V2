package me.nexo.protections.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ClaimAction;
import me.nexo.protections.core.ClaimBox;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.protections.managers.LimitManager;
import me.nexo.protections.menu.ProtectionMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🛡️ NexoProtections - Listener Principal (Arquitectura Enterprise)
 * Rendimiento: Cero I/O en movimiento, Thread-Safe y Gestión de Hilos Virtuales Java 21.
 */
@Singleton
public class ProtectionListener implements Listener {

    private final ClaimManager claimManager;
    private final LimitManager limitManager;
    private final ConfigManager configManager;
    private final NexoProtections plugin;
    private final UserManager userManager;
    private final DatabaseManager databaseManager;
    private final CrossplayUtils crossplayUtils;

    private final NamespacedKey isProtectionStoneKey;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    public ProtectionListener(ClaimManager claimManager, LimitManager limitManager, 
                              ConfigManager configManager, NexoProtections plugin, 
                              UserManager userManager, DatabaseManager databaseManager,
                              CrossplayUtils crossplayUtils) {
        this.claimManager = claimManager;
        this.limitManager = limitManager;
        this.configManager = configManager;
        this.plugin = plugin;
        this.userManager = userManager;
        this.databaseManager = databaseManager;
        this.crossplayUtils = crossplayUtils;

        this.isProtectionStoneKey = new NamespacedKey(plugin, "is_protection_stone");
    }

    // =========================================================================
    // 💥 DESTRUIR UN MONOLITO O ROMPER BLOQUES
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var block = event.getBlock();
        var stone = claimManager.getStoneAt(block.getLocation());

        if (stone == null) return;

        // Si intentan romper el Monolito Central
        if (block.getType() == Material.LODESTONE && block.getLocation().equals(getCenterLocation(stone.getBox()))) {
            if (stone.getOwnerId().equals(player.getUniqueId()) || player.hasPermission("nexoprotections.admin")) {
                
                event.setDropItems(false); // Cancelamos el drop vanilla

                stone.removeHologram();
                claimManager.removeStoneFromCache(stone);

                // Persistencia Asíncrona con Hilos Virtuales
                virtualExecutor.submit(() -> {
                    try (var conn = databaseManager.getConnection();
                         var ps = conn.prepareStatement("DELETE FROM nexo_protections WHERE stone_id = CAST(? AS UUID)")) {
                        ps.setString(1, stone.getStoneId().toString());
                        ps.executeUpdate();
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error eliminando protección: " + e.getMessage());
                    }
                });

                // Creación del Ítem Devuelto (Modernizado con CrossplayUtils)
                var stoneItem = new ItemStack(Material.LODESTONE);
                var meta = stoneItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(crossplayUtils.parseCrossplay(player, configManager.getMessages().mensajes().items().selloAbismoNombre()));
                    meta.lore(configManager.getMessages().mensajes().items().selloAbismoLore().stream()
                            .map(line -> crossplayUtils.parseCrossplay(player, line)).toList());
                    meta.getPersistentDataContainer().set(isProtectionStoneKey, PersistentDataType.BYTE, (byte) 1);
                    stoneItem.setItemMeta(meta);
                }

                block.getWorld().dropItemNaturally(block.getLocation(), stoneItem);
                crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().ritualDeshecho());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
            } else {
                event.setCancelled(true);
                crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().noDestruirAjeno());
            }
            return;
        }

        // Protección de territorio
        if (!stone.getOwnerId().equals(player.getUniqueId()) && !stone.hasPermission(player.getUniqueId(), ClaimAction.BREAK) && !player.hasPermission("nexoprotections.admin")) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().dominioSellado());
        }
    }

    // =========================================================================
    // 🟩 COLOCAR UN MONOLITO O CONSTRUIR BLOQUES
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        var player = event.getPlayer();
        var block = event.getBlockPlaced();
        var existingStone = claimManager.getStoneAt(block.getLocation());

        if (existingStone != null && !existingStone.getOwnerId().equals(player.getUniqueId()) && 
            !existingStone.hasPermission(player.getUniqueId(), ClaimAction.BUILD) && !player.hasPermission("nexoprotections.admin")) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinConstruirAjeno());
            return;
        }

        var itemInHand = event.getItemInHand();
        if (block.getType() == Material.LODESTONE && itemInHand.hasItemMeta() &&
                itemInHand.getItemMeta().getPersistentDataContainer().has(isProtectionStoneKey, PersistentDataType.BYTE)) {

            var loc = block.getLocation();
            int radius = limitManager.getProtectionRadius(player);
            var newBox = new ClaimBox(loc.getWorld().getName(), loc.getBlockX()-radius, -64, loc.getBlockZ()-radius, loc.getBlockX()+radius, 320, loc.getBlockZ()+radius);

            if (claimManager.hasOverlappingClaim(newBox)) {
                event.setCancelled(true);
                crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().colisionEnergia());
                return;
            }

            limitManager.canPlaceNewStone(player).thenAccept(canPlace -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!canPlace) {
                        block.setType(Material.AIR);
                        var refundItem = itemInHand.clone();
                        refundItem.setAmount(1);
                        var sobrante = player.getInventory().addItem(refundItem);
                        if (!sobrante.isEmpty()) player.getWorld().dropItemNaturally(loc, refundItem);
                        crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().limiteAlcanzado());
                        return;
                    }

                    var newStoneId = UUID.randomUUID();
                    var user = userManager.getUserOrNull(player.getUniqueId());
                    UUID clanId = (user != null && user.hasClan()) ? user.getClanId() : null;

                    var newStone = new ProtectionStone(newStoneId, player.getUniqueId(), clanId, newBox);
                    claimManager.addStoneToCache(newStone);
                    newStone.updateHologram();

                    crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().selloInvocado().replace("%radio%", String.valueOf(radius)));
                    player.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1f, 0.5f);

                    virtualExecutor.submit(() -> {
                        String sql = "INSERT INTO nexo_protections (stone_id, owner_id, clan_id, world_name, min_x, min_y, min_z, max_x, max_y, max_z) VALUES (CAST(? AS UUID), CAST(? AS UUID), " + (clanId == null ? "NULL" : "CAST(? AS UUID)") + ", ?, ?, ?, ?, ?, ?, ?)";
                        try (var conn = databaseManager.getConnection();
                             var ps = conn.prepareStatement(sql)) {
                            ps.setString(1, newStoneId.toString());
                            ps.setString(2, player.getUniqueId().toString());
                            int index = 3;
                            if (clanId != null) ps.setString(index++, clanId.toString());
                            ps.setString(index++, newBox.world());
                            ps.setInt(index++, newBox.minX());
                            ps.setInt(index++, newBox.minY());
                            ps.setInt(index++, newBox.minZ());
                            ps.setInt(index++, newBox.maxX());
                            ps.setInt(index++, newBox.maxY());
                            ps.setInt(index++, newBox.maxZ());
                            ps.executeUpdate();
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error guardando piedra: " + e.getMessage());
                        }
                    });
                });
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getClickedBlock() == null) return;

        var block = event.getClickedBlock();
        var player = event.getPlayer();

        if (block.getType() == Material.LODESTONE && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            var stone = claimManager.getStoneAt(block.getLocation());
            if (stone != null && block.getLocation().equals(getCenterLocation(stone.getBox()))) {
                event.setCancelled(true);
                if (stone.getOwnerId().equals(player.getUniqueId()) || stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT) || player.hasPermission("nexoprotections.admin")) {
                    // Inyectamos todas las dependencias requeridas por el constructor de ProtectionMenu
                    new ProtectionMenu(player, stone, configManager, claimManager, crossplayUtils, userManager).open();
                } else {
                    crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().monolitoRechaza());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        var player = event.getPlayer();
        var fromClaim = claimManager.getStoneAt(event.getFrom());
        var toClaim = claimManager.getStoneAt(event.getTo());

        if (fromClaim != toClaim) {
            if (toClaim != null) {
                boolean hasAccess = toClaim.getFlag("ENTRY") ||
                        toClaim.getOwnerId().equals(player.getUniqueId()) ||
                        toClaim.hasPermission(player.getUniqueId(), ClaimAction.INTERACT) ||
                        player.hasPermission("nexoprotections.admin");

                if (!hasAccess) {
                    crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().campoFuerza());
                    var pushback = event.getFrom().toVector().subtract(event.getTo().toVector()).normalize().multiply(0.5);
                    pushback.setY(0.1);
                    player.setVelocity(pushback);
                    event.setCancelled(true);
                    return;
                }

                var ownerUser = userManager.getUserOrNull(toClaim.getOwnerId());
                var ownerName = (ownerUser != null) ? ownerUser.getName() : "Desconocido";
                crossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().exito().zonaProtegida().replace("%owner%", ownerName));
            }

            if (fromClaim != null && toClaim == null) {
                crossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().exito().zonaSalvaje());
            }
        }
    }

    private Location getCenterLocation(ClaimBox box) {
        int centerX = (box.minX() + box.maxX()) / 2;
        int centerZ = (box.minZ() + box.maxZ()) / 2;
        return new Location(Bukkit.getWorld(box.world()), centerX, 0, centerZ);
    }
}
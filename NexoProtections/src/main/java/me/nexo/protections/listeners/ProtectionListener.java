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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

/**
 * 🛡️ NexoProtections - Listener Principal (Arquitectura Enterprise)
 * Rendimiento: Cero I/O en movimiento, Thread-Safe Dupes prevention.
 */
@Singleton
public class ProtectionListener implements Listener {

    private final ClaimManager claimManager;
    private final LimitManager limitManager;
    private final ConfigManager configManager;
    private final NexoProtections plugin;
    private final UserManager userManager;
    private final DatabaseManager databaseManager;

    private final NamespacedKey isProtectionStoneKey;

    // 💉 PILAR 3: Inyección Directa (Desacoplado de NexoCore)
    @Inject
    public ProtectionListener(ClaimManager claimManager, LimitManager limitManager, ConfigManager configManager, NexoProtections plugin, UserManager userManager, DatabaseManager databaseManager) {
        this.claimManager = claimManager;
        this.limitManager = limitManager;
        this.configManager = configManager;
        this.plugin = plugin;
        this.userManager = userManager;
        this.databaseManager = databaseManager;

        this.isProtectionStoneKey = new NamespacedKey(plugin, "is_protection_stone");
    }

    // =========================================================================
    // 💥 DESTRUIR UN MONOLITO O ROMPER BLOQUES
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ProtectionStone stone = claimManager.getStoneAt(block.getLocation());

        if (stone == null) return;

        // Si intentan romper el Monolito Central
        if (block.getType() == Material.LODESTONE && block.getLocation().equals(getCenterLocation(stone.getBox()))) {

            if (stone.getOwnerId().equals(player.getUniqueId()) || player.hasPermission("nexoprotections.admin")) {

                event.setDropItems(false); // Cancelamos el drop vanilla

                // 1. Limpieza de Caché Inmediata (O(1))
                stone.removeHologram();
                claimManager.removeStoneFromCache(stone);

                // 2. Eliminación SQL Asíncrona (Virtual Threads)
                Thread.startVirtualThread(() -> {
                    try (Connection conn = databaseManager.getConnection();
                         PreparedStatement ps = conn.prepareStatement("DELETE FROM nexo_protections WHERE stone_id = CAST(? AS UUID)")) {
                        ps.setString(1, stone.getStoneId().toString());
                        ps.executeUpdate();
                    } catch (Exception e) { e.printStackTrace(); }
                });

                // 3. Creación del Ítem Devuelto
                ItemStack stoneItem = new ItemStack(Material.LODESTONE);
                ItemMeta meta = stoneItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getMessages().mensajes().items().selloAbismoNombre()));
                    meta.lore(configManager.getMessages().mensajes().items().selloAbismoLore().stream()
                            .map(line -> LegacyComponentSerializer.legacyAmpersand().deserialize(line)).toList());
                    meta.getPersistentDataContainer().set(isProtectionStoneKey, PersistentDataType.BYTE, (byte) 1);
                    stoneItem.setItemMeta(meta);
                }

                block.getWorld().dropItemNaturally(block.getLocation(), stoneItem);

                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().ritualDeshecho());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
            } else {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().noDestruirAjeno());
            }
            return;
        }

        // Si intentan romper un bloque CUALQUIERA dentro de la protección
        if (!stone.getOwnerId().equals(player.getUniqueId()) && !stone.hasPermission(player.getUniqueId(), ClaimAction.BREAK) && !player.hasPermission("nexoprotections.admin")) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().dominioSellado());
        }
    }

    // =========================================================================
    // 🟩 COLOCAR UN MONOLITO O CONSTRUIR BLOQUES
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        ProtectionStone existingStone = claimManager.getStoneAt(block.getLocation());

        // Bloquear construcción en claims ajenos
        if (existingStone != null && !existingStone.getOwnerId().equals(player.getUniqueId()) && !existingStone.hasPermission(player.getUniqueId(), ClaimAction.BUILD) && !player.hasPermission("nexoprotections.admin")) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinConstruirAjeno());
            return;
        }

        ItemStack itemInHand = event.getItemInHand();

        // LÓGICA DE CREACIÓN DE NUEVA PROTECCIÓN
        if (block.getType() == Material.LODESTONE && itemInHand.hasItemMeta() &&
                itemInHand.getItemMeta().getPersistentDataContainer().has(isProtectionStoneKey, PersistentDataType.BYTE)) {

            Location loc = block.getLocation();
            int radius = limitManager.getProtectionRadius(player);

            ClaimBox newBox = new ClaimBox(loc.getWorld().getName(), loc.getBlockX()-radius, -64, loc.getBlockZ()-radius, loc.getBlockX()+radius, 320, loc.getBlockZ()+radius);

            // 1. Verificación O(1) de Colisión Espacial
            if (claimManager.hasOverlappingClaim(newBox)) {
                event.setCancelled(true);
                CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().colisionEnergia());
                return;
            }

            // 2. Verificación de Límite (Puede ser asíncrono si llama a base de datos, pero el reembolso debe ser síncrono)
            limitManager.canPlaceNewStone(player).thenAccept(canPlace -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!canPlace) {
                        block.setType(Material.AIR); // Rompemos el bloque físico

                        // 🌟 FIX DUPE: Usamos un ItemStack limpio y dropeamos si el inv está lleno
                        ItemStack refundItem = itemInHand.clone();
                        refundItem.setAmount(1);
                        var sobrante = player.getInventory().addItem(refundItem);
                        if (!sobrante.isEmpty()) player.getWorld().dropItemNaturally(loc, refundItem);

                        CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().limiteAlcanzado());
                        return;
                    }

                    // 3. Oficialización de la Piedra
                    UUID newStoneId = UUID.randomUUID();
                    NexoUser user = userManager.getUserOrNull(player.getUniqueId());
                    UUID clanId = (user != null && user.hasClan()) ? user.getClanId() : null;

                    ProtectionStone newStone = new ProtectionStone(newStoneId, player.getUniqueId(), clanId, newBox);
                    claimManager.addStoneToCache(newStone);
                    newStone.updateHologram();

                    CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().selloInvocado().replace("%radio%", String.valueOf(radius)));
                    player.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1f, 0.5f);

                    // 4. Guardado SQL Asíncrono
                    Thread.startVirtualThread(() -> {
                        String sql = "INSERT INTO nexo_protections (stone_id, owner_id, clan_id, world_name, min_x, min_y, min_z, max_x, max_y, max_z) VALUES (CAST(? AS UUID), CAST(? AS UUID), " + (clanId == null ? "NULL" : "CAST(? AS UUID)") + ", ?, ?, ?, ?, ?, ?, ?)";
                        try (Connection conn = databaseManager.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
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
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                });
            });
        }
    }

    // =========================================================================
    // 🎛️ INTERACTUAR CON EL PANEL (MENÚ)
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.LODESTONE && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ProtectionStone stone = claimManager.getStoneAt(block.getLocation());

            // Verificamos si el bloque clickeado es el centro matemático del claim
            if (stone != null && block.getLocation().equals(getCenterLocation(stone.getBox()))) {
                event.setCancelled(true);

                if (stone.getOwnerId().equals(player.getUniqueId()) || stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT) || player.hasPermission("nexoprotections.admin")) {
                    new ProtectionMenu(player, plugin, stone).open();
                } else {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().monolitoRechaza());
                }
            }
        }
    }

    // =========================================================================
    // 🏃 MOVIMIENTO Y CAMPO DE FUERZA (TPS CRÍTICO)
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        // 🌟 OPTIMIZACIÓN: Solo comprobamos si el jugador cambió de bloque entero, no si solo giró la cámara.
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Búsqueda espacial ultrarrápida O(1)
        ProtectionStone fromClaim = claimManager.getStoneAt(event.getFrom());
        ProtectionStone toClaim = claimManager.getStoneAt(event.getTo());

        if (fromClaim != toClaim) {

            if (toClaim != null) {
                // 🛑 LEY DE ENTRADA: Campo de Fuerza RPG
                boolean hasAccess = toClaim.getFlag("ENTRY") ||
                        toClaim.getOwnerId().equals(player.getUniqueId()) ||
                        toClaim.hasPermission(player.getUniqueId(), ClaimAction.INTERACT) ||
                        player.hasPermission("nexoprotections.admin");

                if (!hasAccess) {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().campoFuerza());

                    Vector pushback = event.getFrom().toVector().subtract(event.getTo().toVector()).normalize().multiply(0.5);
                    pushback.setY(0.1);
                    player.setVelocity(pushback);

                    event.setCancelled(true);
                    return;
                }

                // 🌟 FIX I/O: Obtenemos el nombre desde RAM, NO desde el disco duro (getOfflinePlayer)
                NexoUser ownerUser = userManager.getUserOrNull(toClaim.getOwnerId());
                String ownerName = (ownerUser != null) ? ownerUser.getName() : "Desconocido";

                CrossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().exito().zonaProtegida().replace("%owner%", ownerName));
            }

            if (fromClaim != null && toClaim == null) {
                CrossplayUtils.sendActionBar(player, configManager.getMessages().mensajes().exito().zonaSalvaje());
            }
        }
    }

    // Utilidad Matemática para hallar el bloque central del ClaimBox
    private Location getCenterLocation(ClaimBox box) {
        int centerX = (box.minX() + box.maxX()) / 2;
        int centerZ = (box.minZ() + box.maxZ()) / 2;
        return new Location(Bukkit.getWorld(box.world()), centerX, 0, centerZ);
        // Nota: Asumimos que Y no importa para el bloque físico,
        // pero idealmente deberías guardar la Y exacta de la Lodestone en la base de datos.
    }
}
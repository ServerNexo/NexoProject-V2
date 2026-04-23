package me.nexo.economy.bazar;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.economy.NexoEconomy;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 💰 NexoEconomy - Menú de Órdenes Propias (Arquitectura Enterprise)
 * Rendimiento: Virtual Thread Data Fetching y Folia Region Scheduler Sync.
 */
public class BazaarMyOrdersMenu extends NexoMenu {

    private final NexoEconomy plugin;
    private final BazaarManager bazaarManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 LLAVES ESTÁTICAS PARA OPTIMIZACIÓN PDC
    private static final NamespacedKey KEY_ACTION = new NamespacedKey("nexoeconomy", "action");
    private static final NamespacedKey KEY_ORDER_ID = new NamespacedKey("nexoeconomy", "order_id");

    public BazaarMyOrdersMenu(Player player, NexoEconomy plugin, BazaarManager bazaarManager, CrossplayUtils crossplayUtils) {
        super(player);
        this.plugin = plugin;
        this.bazaarManager = bazaarManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public String getMenuName() {
        return LegacyComponentSerializer.legacySection().serialize(crossplayUtils.parseCrossplay(player, "&#00f5ff📋 <bold>MIS ÓRDENES</bold>"));
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        var loading = new ItemStack(Material.CLOCK);
        loading.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00⏳ <bold>CARGANDO ÓRDENES...</bold>")));
        inventory.setItem(22, loading);

        addBackButton();

        // 🚀 JAVA 21: Carga en hilo virtual para no estresar el pool de Bukkit
        CompletableFuture.supplyAsync(() -> bazaarManager.getMisOrdenes(player.getUniqueId()))
            .thenAccept(orders -> {
                // 🌟 FOLIA SYNC: Regresamos al hilo de la región del jugador para dibujar
                player.getScheduler().run(plugin, task -> {
                    if (player.getOpenInventory().getTopInventory() != inventory) return;

                    inventory.setItem(22, null);

                    int slot = 10;
                    for (var order : orders) {
                        if (slot >= 44) break;

                        var mat = Material.matchMaterial(order.itemId());
                        var item = new ItemStack(mat != null ? mat : Material.STONE);
                        
                        item.editMeta(meta -> {
                            boolean isBuy = order.type().name().equals("BUY");
                            String titulo = isBuy ? "&#55FF55[+] <bold>COMPRA: " + order.itemId() + "</bold>" : "&#FF5555[-] <bold>VENTA: " + order.itemId() + "</bold>";
                            meta.displayName(crossplayUtils.parseCrossplay(player, titulo));

                            var total = order.pricePerUnit().multiply(BigDecimal.valueOf(order.amount()));
                            var lore = List.of(
                                    crossplayUtils.parseCrossplay(player, "&#E6CCFFCantidad: &#00f5ff" + order.amount()),
                                    crossplayUtils.parseCrossplay(player, "&#E6CCFFPrecio Unitario: &#FFAA00" + order.pricePerUnit().toPlainString() + " ⛃"),
                                    crossplayUtils.parseCrossplay(player, "&#E6CCFFTotal: &#FFAA00" + total.toPlainString() + " ⛃"),
                                    crossplayUtils.parseCrossplay(player, ""),
                                    crossplayUtils.parseCrossplay(player, "&#FF3366► Clic para cancelar orden")
                            );
                            meta.lore(lore);

                            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "cancel_order");
                            // Asumiendo que order.orderId() es UUID, lo guardamos como String para el PDC
                            meta.getPersistentDataContainer().set(KEY_ORDER_ID, PersistentDataType.STRING, order.orderId().toString());
                        });

                        inventory.setItem(slot, item);
                        slot++;
                        if (slot % 9 == 8 || slot % 9 == 0) slot += 2;
                    }

                    if (orders.isEmpty()) {
                        var empty = new ItemStack(Material.BARRIER);
                        empty.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555[!] No tienes órdenes activas en el Bazar.")));
                        inventory.setItem(22, empty);
                    }
                    setFillerGlass();
                }, null);
            });
    }

    private void addBackButton() {
        var back = new ItemStack(Material.ARROW);
        back.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF3366⬅ <bold>VOLVER</bold>"));
            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "back_main");
        });
        inventory.setItem(getSlots() - 5, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) return;

        var item = event.getCurrentItem();
        if (item == null || item.isEmpty()) return;

        var meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(KEY_ACTION, PersistentDataType.STRING)) return;

        String action = meta.getPersistentDataContainer().get(KEY_ACTION, PersistentDataType.STRING);

        if ("cancel_order".equals(action)) {
            String orderIdStr = meta.getPersistentDataContainer().get(KEY_ORDER_ID, PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

            player.closeInventory();
            // 🚀 Ejecución asíncrona de cancelación
            bazaarManager.cancelarOrden(player, java.util.UUID.fromString(orderIdStr));
            
            // Reabrimos usando el scheduler de la región para evitar parpadeos en Bedrock
            player.getScheduler().runDelayed(plugin, t -> new BazaarMyOrdersMenu(player, plugin, bazaarManager, crossplayUtils).open(), null, 3L);

        } else if ("back_main".equals(action)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();
            player.getScheduler().runDelayed(plugin, t -> new BazaarMenu(player, bazaarManager, crossplayUtils).open(), null, 3L);
        }
    }
}
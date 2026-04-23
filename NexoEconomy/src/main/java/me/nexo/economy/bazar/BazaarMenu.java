package me.nexo.economy.bazar;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.config.ConfigManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 💰 NexoEconomy - Menú de Navegación del Bazar (Arquitectura Enterprise)
 * Rendimiento: Folia Region Scheduler, Virtual Threads Handover y Dependencias Transitivas.
 * Nota: Instanciado dinámicamente por jugador. NO usa @Singleton.
 */
public class BazaarMenu extends NexoMenu {

    // 🌟 DEPENDENCIAS PROPAGADAS DESDE EL COMANDO/LISTENER
    private final NexoEconomy plugin;
    private final BazaarManager bazaarManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;
    
    private final MenuType type;
    private final String category;
    private final String selectedItem;

    public enum MenuType { MAIN, CATEGORY, ITEM_OPTIONS }

    // 🌟 LLAVES ESTÁTICAS PDC (Ahorro de RAM)
    private static final NamespacedKey KEY_ACTION = new NamespacedKey("nexoeconomy", "action");
    private static final NamespacedKey KEY_CATEGORY = new NamespacedKey("nexoeconomy", "category");
    private static final NamespacedKey KEY_ITEM_ID = new NamespacedKey("nexoeconomy", "item_id");

    public BazaarMenu(Player player, NexoEconomy plugin, BazaarManager bazaarManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this(player, plugin, bazaarManager, configManager, crossplayUtils, MenuType.MAIN, "", "");
    }

    public BazaarMenu(Player player, NexoEconomy plugin, BazaarManager bazaarManager, ConfigManager configManager, CrossplayUtils crossplayUtils, String category) {
        this(player, plugin, bazaarManager, configManager, crossplayUtils, MenuType.CATEGORY, category, "");
    }

    public BazaarMenu(Player player, NexoEconomy plugin, BazaarManager bazaarManager, ConfigManager configManager, CrossplayUtils crossplayUtils, String category, String itemId) {
        this(player, plugin, bazaarManager, configManager, crossplayUtils, MenuType.ITEM_OPTIONS, category, itemId);
    }

    private BazaarMenu(Player player, NexoEconomy plugin, BazaarManager bazaarManager, ConfigManager configManager, CrossplayUtils crossplayUtils, MenuType type, String category, String selectedItem) {
        super(player);
        this.plugin = plugin;
        this.bazaarManager = bazaarManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        
        this.type = type;
        this.category = category;
        this.selectedItem = selectedItem;
    }

    @Override
    public String getMenuName() {
        if (type == MenuType.MAIN) return LegacyComponentSerializer.legacySection().serialize(crossplayUtils.parseCrossplay(player, configManager.getMessages().menus().bazarTitulo()));
        if (type == MenuType.CATEGORY) return LegacyComponentSerializer.legacySection().serialize(crossplayUtils.parseCrossplay(player, "&#FFAA00⚖ <bold>CATEGORÍA: " + category + "</bold>"));
        return LegacyComponentSerializer.legacySection().serialize(crossplayUtils.parseCrossplay(player, "&#FFAA00⚖ <bold>MERCADO: " + selectedItem + "</bold>"));
    }

    @Override
    public int getSlots() {
        return type == MenuType.ITEM_OPTIONS ? 45 : 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        if (type == MenuType.MAIN) {
            var mineria = new ItemStack(Material.DIAMOND_PICKAXE);
            mineria.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff⛏ <bold>MINERÍA</bold>"));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "open_category");
                meta.getPersistentDataContainer().set(KEY_CATEGORY, PersistentDataType.STRING, "MINING");
            });

            var farmeo = new ItemStack(Material.GOLDEN_HOE);
            farmeo.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55🌾 <bold>AGRICULTURA</bold>"));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "open_category");
                meta.getPersistentDataContainer().set(KEY_CATEGORY, PersistentDataType.STRING, "FARMING");
            });

            inventory.setItem(20, mineria);
            inventory.setItem(24, farmeo);

            var buzon = new ItemStack(Material.CHEST);
            buzon.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00📦 <bold>RECLAMAR ENTREGAS</bold>"));
                meta.lore(List.of(crossplayUtils.parseCrossplay(player, "&#E6CCFFClic para recoger tus compras o ventas completadas.")));
                meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "claim_deliveries");
            });

            var misOrdenes = new ItemStack(Material.PAPER);
            misOrdenes.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff📋 <bold>MIS ÓRDENES</bold>"));
                meta.lore(List.of(crossplayUtils.parseCrossplay(player, "&#E6CCFFClic para ver o cancelar tus órdenes activas.")));
                meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "open_my_orders");
            });

            inventory.setItem(40, buzon);
            inventory.setItem(41, misOrdenes);

        } else if (type == MenuType.CATEGORY) {
            var loading = new ItemStack(Material.CLOCK);
            loading.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00⏳ <bold>CARGANDO MERCADO...</bold>")));
            inventory.setItem(22, loading);

            addBackButton("main");

            // 🚀 JAVA 21: Carga asíncrona no bloqueante
            CompletableFuture.supplyAsync(() -> {
                List<ItemStack> items = new ArrayList<>();
                if (category.equals("MINING")) {
                    items.add(buildBazaarItem(Material.COAL));
                    items.add(buildBazaarItem(Material.IRON_INGOT));
                    items.add(buildBazaarItem(Material.GOLD_INGOT));
                    items.add(buildBazaarItem(Material.DIAMOND));
                    items.add(buildBazaarItem(Material.OBSIDIAN));
                } else if (category.equals("FARMING")) {
                    items.add(buildBazaarItem(Material.WHEAT));
                    items.add(buildBazaarItem(Material.CARROT));
                    items.add(buildBazaarItem(Material.POTATO));
                    items.add(buildBazaarItem(Material.SUGAR_CANE));
                }
                return items;
            }).thenAccept(items -> {
                // 🌟 FOLIA SYNC: Sincronización exacta con la región del jugador
                player.getScheduler().run(plugin, task -> {
                    if (player.getOpenInventory().getTopInventory() != inventory) return;

                    inventory.setItem(22, null);
                    int slot = 10;
                    for (var it : items) {
                        inventory.setItem(slot, it);
                        slot++;
                        if (slot == 17 || slot == 26 || slot == 35) slot += 2;
                    }
                    setFillerGlass();
                }, null);
            });

        } else if (type == MenuType.ITEM_OPTIONS) {
            Material mat = Material.matchMaterial(selectedItem);
            if (mat == null) mat = Material.STONE;

            var buy = new ItemStack(Material.GREEN_TERRACOTTA);
            buy.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55[+] <bold>CREAR ORDEN DE COMPRA</bold>"));
                meta.lore(List.of(crossplayUtils.parseCrossplay(player, "&#E6CCFFOfrece monedas a cambio de ítems.")));
                meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "create_buy_order");
                meta.getPersistentDataContainer().set(KEY_ITEM_ID, PersistentDataType.STRING, selectedItem);
            });

            var sell = new ItemStack(Material.RED_TERRACOTTA);
            sell.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555[-] <bold>CREAR ORDEN DE VENTA</bold>"));
                meta.lore(List.of(crossplayUtils.parseCrossplay(player, "&#E6CCFFOfrece ítems a cambio de monedas.")));
                meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "create_sell_order");
                meta.getPersistentDataContainer().set(KEY_ITEM_ID, PersistentDataType.STRING, selectedItem);
            });

            inventory.setItem(13, new ItemStack(mat));
            inventory.setItem(20, buy);
            inventory.setItem(24, sell);

            addBackButton("cat_" + category);
        }
    }

    private ItemStack buildBazaarItem(Material mat) {
        String itemId = mat.name();
        BigDecimal buyPrice = bazaarManager.getMejorPrecioVenta(itemId); // Lo que le cuesta al comprador
        BigDecimal sellPrice = bazaarManager.getMejorPrecioCompra(itemId); // Lo que recibe el vendedor
        int buyOrders = bazaarManager.getVolumenOrdenes(itemId, "BUY");
        int sellOrders = bazaarManager.getVolumenOrdenes(itemId, "SELL");

        var item = new ItemStack(mat);
        item.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00<bold>" + mat.name() + "</bold>"));

            String bpStr = buyPrice.compareTo(BigDecimal.ZERO) == 0 ? "N/A" : buyPrice.toPlainString();
            String spStr = sellPrice.compareTo(BigDecimal.ZERO) == 0 ? "N/A" : sellPrice.toPlainString();
            String marginStr = (buyPrice.compareTo(BigDecimal.ZERO) > 0 && sellPrice.compareTo(BigDecimal.ZERO) > 0) ? buyPrice.subtract(sellPrice).toPlainString() : "N/A";

            var loreRaw = List.of(
                    "&#E6CCFFMejor Oferta (Compra): &#55FF55" + bpStr + " ⛃",
                    "&#E6CCFFMejor Demanda (Venta): &#FF5555" + spStr + " ⛃",
                    "&#E6CCFFMargen: &#00f5ff" + marginStr + " ⛃",
                    "",
                    "&#E6CCFFVolumen de Compra: &#55FF55" + buyOrders,
                    "&#E6CCFFVolumen de Venta: &#FF5555" + sellOrders,
                    "",
                    "&#00f5ff► Clic para comerciar"
            );

            meta.lore(loreRaw.stream()
                    .map(line -> crossplayUtils.parseCrossplay(player, line))
                    .toList());

            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "open_options");
            meta.getPersistentDataContainer().set(KEY_ITEM_ID, PersistentDataType.STRING, itemId);
        });

        return item;
    }

    private void addBackButton(String target) {
        var back = new ItemStack(Material.ARROW);
        back.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF3366⬅ <bold>VOLVER</bold>"));
            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, "back_" + target);
        });
        inventory.setItem(getSlots() - 5, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) return;

        var item = event.getCurrentItem();
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(KEY_ACTION, PersistentDataType.STRING)) return;
        String action = meta.getPersistentDataContainer().get(KEY_ACTION, PersistentDataType.STRING);

        if ("open_category".equals(action)) {
            String cat = meta.getPersistentDataContainer().get(KEY_CATEGORY, PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            player.getScheduler().runDelayed(plugin, t -> new BazaarMenu(player, plugin, bazaarManager, configManager, crossplayUtils, cat).open(), null, 3L);

        } else if ("open_options".equals(action)) {
            String itemId = meta.getPersistentDataContainer().get(KEY_ITEM_ID, PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            player.getScheduler().runDelayed(plugin, t -> new BazaarMenu(player, plugin, bazaarManager, configManager, crossplayUtils, category, itemId).open(), null, 3L);

        } else if ("claim_deliveries".equals(action)) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
            bazaarManager.reclamarBuzon(player);

        } else if (action.startsWith("back_")) {
            String target = action.replace("back_", "");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();

            player.getScheduler().runDelayed(plugin, t -> {
                if (target.equals("main")) {
                    new BazaarMenu(player, plugin, bazaarManager, configManager, crossplayUtils).open();
                } else if (target.startsWith("cat_")) {
                    new BazaarMenu(player, plugin, bazaarManager, configManager, crossplayUtils, target.replace("cat_", "")).open();
                }
            }, null, 3L);

        } else if ("open_my_orders".equals(action)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            player.getScheduler().runDelayed(plugin, t -> new BazaarMyOrdersMenu(player, plugin, bazaarManager, crossplayUtils).open(), null, 3L);

        } else if ("create_buy_order".equals(action) || "create_sell_order".equals(action)) {
            String itemId = meta.getPersistentDataContainer().get(KEY_ITEM_ID, PersistentDataType.STRING);
            player.closeInventory();

            String orderType = "create_buy_order".equals(action) ? "BUY" : "SELL";
            
            // 🌟 Invocamos al Gestor para registrar la sesión en lugar de llamar al mapa estático del listener
            bazaarManager.iniciarSesionChat(player.getUniqueId(), itemId, orderType);

            crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            crossplayUtils.sendMessage(player, "&#FFAA00⚖ <bold>ORDEN DEL BAZAR: " + itemId + "</bold>");
            crossplayUtils.sendMessage(player, "&#E6CCFFEscribe en el chat la cantidad y el precio deseado.");
            crossplayUtils.sendMessage(player, "&#00f5ffFormato: &#E6CCFF<cantidad> <precio_por_unidad>");
            crossplayUtils.sendMessage(player, "&#E6CCFFEjemplo para comerciar 64 ítems a 10 Monedas c/u: &#55FF5564 10");
            crossplayUtils.sendMessage(player, "&#E6CCFFEscribe &#FF5555cancelar &#E6CCFFpara abortar la operación.");
            crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}
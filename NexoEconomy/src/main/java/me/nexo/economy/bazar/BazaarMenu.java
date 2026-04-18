package me.nexo.economy.bazar;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.economy.NexoEconomy;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 💰 NexoEconomy - Menú de Navegación del Bazar (Arquitectura Enterprise)
 * Nota: Los menús son instanciados por jugador, NO usan @Singleton.
 */
public class BazaarMenu extends NexoMenu {

    private final NexoEconomy plugin;
    private final MenuType type;
    private final String category;
    private final String selectedItem;

    public enum MenuType { MAIN, CATEGORY, ITEM_OPTIONS }

    // 🌟 Constructores inteligentes para cada pantalla
    public BazaarMenu(Player player, NexoEconomy plugin) {
        this(player, plugin, MenuType.MAIN, "", "");
    }

    public BazaarMenu(Player player, NexoEconomy plugin, String category) {
        this(player, plugin, MenuType.CATEGORY, category, "");
    }

    public BazaarMenu(Player player, NexoEconomy plugin, String category, String itemId) {
        this(player, plugin, MenuType.ITEM_OPTIONS, category, itemId);
    }

    private BazaarMenu(Player player, NexoEconomy plugin, MenuType type, String category, String selectedItem) {
        super(player);
        this.plugin = plugin;
        this.type = type;
        this.category = category;
        this.selectedItem = selectedItem;
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Títulos Hexadecimales directos compatibles con Bedrock
        if (type == MenuType.MAIN) return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(plugin.getConfigManager().getMessages().menus().bazarTitulo()));
        if (type == MenuType.CATEGORY) return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#FFAA00⚖ <bold>CATEGORÍA: " + category + "</bold>"));
        return LegacyComponentSerializer.legacySection().serialize(NexoColor.parse("&#FFAA00⚖ <bold>MERCADO: " + selectedItem + "</bold>"));
    }

    @Override
    public int getSlots() {
        return type == MenuType.ITEM_OPTIONS ? 45 : 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        if (type == MenuType.MAIN) {
            ItemStack mineria = new ItemStack(Material.DIAMOND_PICKAXE);
            ItemMeta meta = mineria.getItemMeta();
            meta.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff⛏ <bold>MINERÍA</bold>"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_category");
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "category"), PersistentDataType.STRING, "MINING");
            mineria.setItemMeta(meta);

            ItemStack farmeo = new ItemStack(Material.GOLDEN_HOE);
            ItemMeta meta2 = farmeo.getItemMeta();
            meta2.displayName(CrossplayUtils.parseCrossplay(player, "&#55FF55🌾 <bold>AGRICULTURA</bold>"));
            meta2.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta2.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_category");
            meta2.getPersistentDataContainer().set(new NamespacedKey(plugin, "category"), PersistentDataType.STRING, "FARMING");
            farmeo.setItemMeta(meta2);

            inventory.setItem(20, mineria);
            inventory.setItem(24, farmeo);

            ItemStack buzon = new ItemStack(Material.CHEST);
            ItemMeta metaBuzon = buzon.getItemMeta();
            metaBuzon.displayName(CrossplayUtils.parseCrossplay(player, "&#FFAA00📦 <bold>RECLAMAR ENTREGAS</bold>"));
            metaBuzon.lore(List.of(CrossplayUtils.parseCrossplay(player, "&#E6CCFFClic para recoger tus compras o ventas completadas.")));
            metaBuzon.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "claim_deliveries");
            buzon.setItemMeta(metaBuzon);

            ItemStack misOrdenes = new ItemStack(Material.PAPER);
            ItemMeta metaOrdenes = misOrdenes.getItemMeta();
            metaOrdenes.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff📋 <bold>MIS ÓRDENES</bold>"));
            metaOrdenes.lore(List.of(CrossplayUtils.parseCrossplay(player, "&#E6CCFFClic para ver o cancelar tus órdenes activas.")));
            metaOrdenes.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_my_orders");
            misOrdenes.setItemMeta(metaOrdenes);

            inventory.setItem(40, buzon);
            inventory.setItem(41, misOrdenes);

        } else if (type == MenuType.CATEGORY) {
            ItemStack loading = new ItemStack(Material.CLOCK);
            ItemMeta lMeta = loading.getItemMeta();
            lMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#FFAA00⏳ <bold>CARGANDO MERCADO...</bold>"));
            loading.setItemMeta(lMeta);
            inventory.setItem(22, loading);

            addBackButton("main");

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.getOpenInventory().getTopInventory() != inventory) return;

                    inventory.setItem(22, new ItemStack(Material.AIR));
                    int slot = 10;
                    for (ItemStack it : items) {
                        inventory.setItem(slot, it);
                        slot++;
                        if (slot == 17 || slot == 26 || slot == 35) slot += 2;
                    }
                    setFillerGlass();
                });
            });

        } else if (type == MenuType.ITEM_OPTIONS) {
            Material mat = Material.matchMaterial(selectedItem);
            if (mat == null) mat = Material.STONE;

            ItemStack buy = new ItemStack(Material.GREEN_TERRACOTTA);
            ItemMeta metaBuy = buy.getItemMeta();
            metaBuy.displayName(CrossplayUtils.parseCrossplay(player, "&#55FF55[+] <bold>CREAR ORDEN DE COMPRA</bold>"));
            metaBuy.lore(List.of(CrossplayUtils.parseCrossplay(player, "&#E6CCFFOfrece monedas a cambio de ítems.")));
            metaBuy.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "create_buy_order");
            metaBuy.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, selectedItem);
            buy.setItemMeta(metaBuy);

            ItemStack sell = new ItemStack(Material.RED_TERRACOTTA);
            ItemMeta metaSell = sell.getItemMeta();
            metaSell.displayName(CrossplayUtils.parseCrossplay(player, "&#FF5555[-] <bold>CREAR ORDEN DE VENTA</bold>"));
            metaSell.lore(List.of(CrossplayUtils.parseCrossplay(player, "&#E6CCFFOfrece ítems a cambio de monedas.")));
            metaSell.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "create_sell_order");
            metaSell.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, selectedItem);
            sell.setItemMeta(metaSell);

            inventory.setItem(13, new ItemStack(mat));
            inventory.setItem(20, buy);
            inventory.setItem(24, sell);

            addBackButton("cat_" + category);
        }
    }

    private ItemStack buildBazaarItem(Material mat) {
        String itemId = mat.name();
        BigDecimal buyPrice = plugin.getBazaarManager().getMejorPrecioVenta(itemId); // Lo que le cuesta al comprador
        BigDecimal sellPrice = plugin.getBazaarManager().getMejorPrecioCompra(itemId); // Lo que recibe el vendedor
        int buyOrders = plugin.getBazaarManager().getVolumenOrdenes(itemId, "BUY");
        int sellOrders = plugin.getBazaarManager().getVolumenOrdenes(itemId, "SELL");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#FFAA00<bold>" + mat.name() + "</bold>"));

        String bpStr = buyPrice.compareTo(BigDecimal.ZERO) == 0 ? "N/A" : buyPrice.toString();
        String spStr = sellPrice.compareTo(BigDecimal.ZERO) == 0 ? "N/A" : sellPrice.toString();
        String marginStr = (buyPrice.compareTo(BigDecimal.ZERO) > 0 && sellPrice.compareTo(BigDecimal.ZERO) > 0) ? buyPrice.subtract(sellPrice).toString() : "N/A";

        // 🌟 FIX: Lore construido directamente en Componentes
        List<String> loreRaw = List.of(
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
                .map(line -> LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(line)))
                .map(line -> CrossplayUtils.parseCrossplay(player, line))
                .collect(Collectors.toList()));

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_options");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, itemId);
        item.setItemMeta(meta);

        return item;
    }

    private void addBackButton(String target) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#FF3366⬅ <bold>VOLVER</bold>"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "back_" + target);
        back.setItemMeta(meta);
        inventory.setItem(getSlots() - 5, back);
    }

    // 🌟 MOTOR DE CLICS UNIFICADO
    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto para protección de transacciones
        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        // Si el ítem no tiene nuestra metadata incrustada, lo ignoramos
        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        if (action.equals("open_category")) {
            String cat = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "category"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> new BazaarMenu(player, plugin, cat).open(), 3L);

        } else if (action.equals("open_options")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> new BazaarMenu(player, plugin, category, itemId).open(), 3L);

        } else if (action.equals("claim_deliveries")) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
            plugin.getBazaarManager().reclamarBuzon(player);

        } else if (action.startsWith("back_")) {
            String target = action.replace("back_", "");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (target.equals("main")) {
                    new BazaarMenu(player, plugin).open();
                } else if (target.startsWith("cat_")) {
                    new BazaarMenu(player, plugin, target.replace("cat_", "")).open();
                }
            }, 3L);

        } else if (action.equals("open_my_orders")) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Instancia el submenú personal
                new BazaarMyOrdersMenu(player, plugin).open();
            }, 3L);

        } else if (action.equals("create_buy_order") || action.equals("create_sell_order")) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
            player.closeInventory();

            String orderType = action.equals("create_buy_order") ? "BUY" : "SELL";
            BazaarChatListener.activeSessions.put(player.getUniqueId(), new BazaarChatListener.OrderSession(itemId, orderType));

            // 🌟 FIX: Textos directos
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            CrossplayUtils.sendMessage(player, "&#FFAA00⚖ <bold>ORDEN DEL BAZAR: " + itemId + "</bold>");
            CrossplayUtils.sendMessage(player, "&#E6CCFFEscribe en el chat la cantidad y el precio deseado.");
            CrossplayUtils.sendMessage(player, "&#00f5ffFormato: &#E6CCFF<cantidad> <precio_por_unidad>");
            CrossplayUtils.sendMessage(player, "&#E6CCFFEjemplo para comerciar 64 ítems a 10 Monedas c/u: &#55FF5564 10");
            CrossplayUtils.sendMessage(player, "&#E6CCFFEscribe &#FF5555cancelar &#E6CCFFpara abortar la operación.");
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}
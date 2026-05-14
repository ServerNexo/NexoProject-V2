package me.aeroxis.factories.menu;

import com.nexomc.nexo.api.NexoItems;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.factories.AeroxisFactories;
import me.aeroxis.factories.managers.SiloManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer; // 🌟 IMPORT AÑADIDO
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ☁️ Terminal VIP - Menú del Silo en la Nube
 */
public class SiloMenu extends AeroxisMenu {

    private final AeroxisFactories plugin;
    private final SiloManager siloManager;

    private Map<String, Long> cachedItems = null;
    private int page = 1;
    private int maxPages = 1;

    private final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public SiloMenu(Player player, AeroxisFactories plugin, CrossplayUtils crossplayUtils, SiloManager siloManager) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.siloManager = siloManager;
    }

    // 🌟 SOBRESCRIBIMOS OPEN() PARA CARGAR LA DB ANTES DE MOSTRAR EL MENÚ
    @Override
    public void open() {
        crossplayUtils.sendMessage(player, "&#00f5ff[☁] Sincronizando con la red del Nexo...");

        siloManager.getPlayerSilo(player.getUniqueId()).thenAccept(data -> {
            this.cachedItems = data;
            this.maxPages = Math.max(1, (int) Math.ceil((double) data.size() / ITEM_SLOTS.length));

            // Volvemos al Hilo Principal para abrir el inventario Bukkit
            Bukkit.getRegionScheduler().execute(plugin, player.getLocation(), super::open);
        });
    }

    @Override
    public String getMenuName() {
        return "&#00f5ff☁ Silo Virtual (Pág. " + page + "/" + maxPages + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Rellena el fondo con cristal

        if (cachedItems == null || cachedItems.isEmpty()) {
            setItem(22, Material.MINECART, "&#FF5555Silo Vacío", List.of(
                    "&#E6CCFFTu nube logística no tiene recursos.",
                    "&#E6CCFFEnlaza un Minion o Fábrica usando",
                    "&#E6CCFFtu Vara Logística y el comando /silo link"
            ));
            return;
        }

        // 🌟 RENDERIZADO PAGINADO
        List<Map.Entry<String, Long>> itemList = new ArrayList<>(cachedItems.entrySet());
        int startIndex = (page - 1) * ITEM_SLOTS.length;
        int endIndex = Math.min(startIndex + ITEM_SLOTS.length, itemList.size());

        int slotIndex = 0;
        DecimalFormat format = new DecimalFormat("#,###");

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Long> entry = itemList.get(i);
            String itemId = entry.getKey();
            long amount = entry.getValue();

            ItemStack displayItem = getVisualItem(itemId);

            displayItem.editMeta(meta -> {
                String cleanName = itemId.replace("_", " ");
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff<bold>" + cleanName.toUpperCase() + "</bold>"));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFAlmacenado: &#55FF55" + format.format(amount)));
                lore.add(Component.empty());
                lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Clic Izquierdo: &#E6CCFFRetirar x64"));
                lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Clic Derecho: &#E6CCFFRetirar Todo (Inv)"));
                meta.lore(lore);
            });

            inventory.setItem(ITEM_SLOTS[slotIndex], displayItem);
            slotIndex++;
        }

        // Botones de Paginación
        if (page > 1) setItem(48, Material.ARROW, "&#FFAA00◀ Página Anterior", null);
        if (page < maxPages) setItem(50, Material.ARROW, "&#FFAA00Página Siguiente ▶", null);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        int slot = e.getRawSlot();

        if (slot == 48 && page > 1) {
            page--;
            super.open(); // Refresca GUI
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }
        if (slot == 50 && page < maxPages) {
            page++;
            super.open(); // Refresca GUI
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // 🌟 LÓGICA DE RETIRO
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        // 🌟 FIX: Extracción del texto plano usando la API de Kyori Adventure
        String plainName = "";
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            plainName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
        }

        if (plainName.isEmpty() || plainName.contains("Silo Vacío") || plainName.contains("Página")) return;

        // Reconstruimos la ID desde el nombre que pusimos (O puedes guardarlo en NBT para ser 100% seguro)
        String targetItemId = plainName.replace(" ", "_").toUpperCase();

        int amountToWithdraw = e.isRightClick() ? calcularEspacioVacio(targetItemId) : 64;
        if (amountToWithdraw <= 0) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] No tienes espacio en el inventario.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Bloqueamos el menú temporalmente para evitar spam clicks
        player.closeInventory();
        crossplayUtils.sendMessage(player, "&#FFAA00[☁] Extrayendo recursos de la nube...");

        siloManager.withdrawItem(player.getUniqueId(), targetItemId, amountToWithdraw).thenAccept(withdrawn -> {
            Bukkit.getRegionScheduler().execute(plugin, player.getLocation(), () -> {
                if (withdrawn > 0) {
                    ItemStack itemToGive = getVisualItem(targetItemId);
                    itemToGive.setAmount(withdrawn);
                    player.getInventory().addItem(itemToGive);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 2f);
                    crossplayUtils.sendMessage(player, "&#55FF55[✓] Has retirado " + withdrawn + " unidades.");
                } else {
                    crossplayUtils.sendMessage(player, "&#FF5555[x] Error al retirar el ítem (Fondos insuficientes).");
                }
                // Re-abrimos el menú para que vea los datos actualizados
                this.open();
            });
        });
    }

    // ==========================================
    // UTILS
    // ==========================================
    private ItemStack getVisualItem(String id) {
        var nexoItem = NexoItems.itemFromId(id.toLowerCase());
        if (nexoItem != null) return nexoItem.build();
        try {
            return new ItemStack(Material.valueOf(id.toUpperCase()));
        } catch (Exception e) {
            return new ItemStack(Material.STONE);
        }
    }

    private int calcularEspacioVacio(String id) {
        ItemStack sample = getVisualItem(id);
        int maxStack = sample.getMaxStackSize();
        int space = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                space += maxStack;
            } else if (item.isSimilar(sample) && item.getAmount() < maxStack) {
                space += (maxStack - item.getAmount());
            }
        }
        return space;
    }
}
package me.nexo.core.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public abstract class NexoMenu implements InventoryHolder {

    protected Inventory inventory;
    protected Player player;

    public NexoMenu(Player player) {
        this.player = player;
    }

    // 🌟 Obligamos a que cualquier menú que crees en el futuro tenga estas 4 cosas:
    public abstract String getMenuName();
    public abstract int getSlots();
    public abstract void handleMenu(InventoryClickEvent e);
    public abstract void setMenuItems();

    // 🌟 NUEVO: Método opcional para detectar cuando se cierra el menú.
    // Lo dejamos vacío aquí para que los menús que NO lo necesiten no den error.
    public void handleClose(InventoryCloseEvent e) {
    }

    // 🌟 El método universal para abrir el menú
    public void open() {
        int size = CrossplayUtils.getOptimizedMenuSize(player, getSlots());
        inventory = Bukkit.createInventory(this, size, CrossplayUtils.parseCrossplay(player, getMenuName()));

        this.setMenuItems();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // 🛠️ HERRAMIENTA CÓMODA: Para crear ítems en 1 sola línea de código
    protected void setItem(int slot, Material material, String nombre, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(CrossplayUtils.parseCrossplay(player, nombre));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(player, line))
                        .collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    // 🛠️ HERRAMIENTA CÓMODA: Para rellenar huecos vacíos con cristal
    protected void setFillerGlass() {
        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(CrossplayUtils.parseCrossplay(player, " "));
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < getSlots(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
}
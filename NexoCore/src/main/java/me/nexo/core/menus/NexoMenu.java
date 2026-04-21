package me.nexo.core.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🏛️ Nexo Network - Base de Menús (Arquitectura Enterprise)
 * Clase abstracta base para la creación de GUIs. No es un Singleton,
 * se instancia por jugador recibiendo dependencias inyectadas desde las implementaciones.
 */
public abstract class NexoMenu implements InventoryHolder {

    protected Inventory inventory;
    protected final Player player;
    
    // 💉 PILAR 1: Desacoplamiento estricto. La utilidad se pasa por constructor.
    protected final CrossplayUtils crossplayUtils; 

    public NexoMenu(Player player, CrossplayUtils crossplayUtils) {
        this.player = player;
        this.crossplayUtils = crossplayUtils;
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
        int size = crossplayUtils.getOptimizedMenuSize(player, getSlots());
        inventory = Bukkit.createInventory(this, size, crossplayUtils.parseCrossplay(player, getMenuName()));

        this.setMenuItems();
        player.openInventory(inventory);
    }

    // 🌟 FIX CRÍTICO PAPER 1.21.5: Contrato de nulabilidad estricto
    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // 🛠️ HERRAMIENTA CÓMODA: Para crear ítems en 1 sola línea de código
    protected void setItem(int slot, Material material, String nombre, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(crossplayUtils.parseCrossplay(player, nombre));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(line -> crossplayUtils.parseCrossplay(player, line))
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
            // 🌟 NATIVO PAPER: Usamos un componente vacío directamente sin parsear strings
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < getSlots(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
}
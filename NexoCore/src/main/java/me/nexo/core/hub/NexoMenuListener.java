package me.nexo.core.hub;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 🏛️ Nexo Network - Listener del Menú Principal (Hub)
 * Arquitectura Enterprise: Cero métodos estáticos, validaciones modernas Paper 1.21.5.
 */
@Singleton
public class NexoMenuListener implements Listener {

    private final NexoCore plugin;
    private final NexoColor nexoColor;
    private final CrossplayUtils crossplayUtils;
    private final NamespacedKey menuKey;

    // 💉 PILAR 1: Inyección estricta de dependencias
    @Inject
    public NexoMenuListener(NexoCore plugin, NexoColor nexoColor, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.nexoColor = nexoColor;
        this.crossplayUtils = crossplayUtils;
        this.menuKey = new NamespacedKey(plugin, "is_nexo_menu");
    }

    public ItemStack getMenuItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(nexoColor.parse("&#ff00ff<bold>Menú Principal</bold> &#E6CCFF(Clic Derecho)"));
            List<Component> lore = new ArrayList<>();
            lore.add(nexoColor.parse("&#E6CCFFTu conexión directa con el Nexo."));
            lore.add(Component.empty());
            lore.add(nexoColor.parse("&#00f5ff¡Clic derecho para abrir!"));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(menuKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { 
        giveMenu(event.getPlayer()); 
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // ⚡ UI Safety: Las manipulaciones de inventario DEBEN ocurrir en el Main Thread.
        // Un delay de 2 ticks síncrono es la práctica correcta aquí.
        Bukkit.getScheduler().runTaskLater(plugin, () -> giveMenu(event.getPlayer()), 2L);
    }

    private void giveMenu(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isMenuItem(item)) player.getInventory().setItem(i, null);
        }
        player.getInventory().setItem(8, getMenuItem());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isMenuItem(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Iterator<ItemStack> iter = event.getDrops().iterator();
        while (iter.hasNext()) {
            ItemStack drop = iter.next();
            if (isMenuItem(drop)) iter.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        // 🌟 Paper 1.21.5: Verificación de acciones mucho más limpia
        if (event.getAction().isRightClick() || event.getAction().isLeftClick()) {

            ItemStack item = event.getItem();
            if (isMenuItem(item)) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                player.closeInventory();

                // 🌟 INSTANCIACIÓN OMEGA: Pasamos la utilidad inyectada al menú
                new HubMenu(player, plugin, crossplayUtils).open();
            }
        }
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        // 🛡️ Solo mantenemos la protección para que el jugador no pueda mover o tirar el reloj
        if (event.getCurrentItem() != null && isMenuItem(event.getCurrentItem())) event.setCancelled(true);
        if (event.getCursor() != null && isMenuItem(event.getCursor())) event.setCancelled(true);
        
        // 🌟 Verificación estricta de ClickType
        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (isMenuItem(hotbarItem)) event.setCancelled(true);
        }
    }

    private boolean isMenuItem(ItemStack item) {
        // 🌟 Paper 1.21.5: item.isEmpty() reemplaza item.getType() == Material.AIR
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(menuKey, PersistentDataType.BYTE);
    }
}
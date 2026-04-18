package me.nexo.core.hub;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class HubMenu extends NexoMenu {

    private final NexoCore plugin;

    public HubMenu(Player player, NexoCore plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "&#ff00ff🌌 Red del Nexo";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // El cristal morado automático de la Arquitectura Omega

        inventory.setItem(13, createPlayerProfile());

        inventory.setItem(20, createButton(Material.DIAMOND_SWORD, "&#00f5ff⚔️ Habilidades", "Sube de nivel tus profesiones.", "open_skills"));
        inventory.setItem(21, createButton(Material.WRITABLE_BOOK, "&#00f5ff📚 Colecciones", "Abre tu grimorio de progresión.", "open_colecciones"));
        inventory.setItem(22, createButton(Material.CRAFTING_TABLE, "&#ff00ff📖 Libro de Recetas", "Descubre crafteos de artefactos.", "open_recipes"));
        inventory.setItem(23, createButton(Material.GOLD_INGOT, "&#ff00ff📈 Bazar Global", "Comercia materiales con operarios.", "open_bazar"));
        inventory.setItem(24, createButton(Material.ZOMBIE_HEAD, "&#8b0000💀 Cacerías (Slayers)", "Invoca jefes y reclama sus almas.", "open_slayer"));

        inventory.setItem(29, createButton(Material.ENDER_CHEST, "&#ff00ff🎒 Almacenamiento", "Abre tus mochilas remotas.", "open_pv"));
        inventory.setItem(30, createButton(Material.LEATHER_CHESTPLATE, "&#ff00ff👕 Guardarropa", "Accede a tus armaduras.", "open_wardrobe"));
        inventory.setItem(31, createButton(Material.COMPASS, "&#00f5ff🌍 Viaje Rápido", "Teletransporte a zonas seguras.", "open_fast_travel"));
        inventory.setItem(32, createButton(Material.SHIELD, "&#00f5ff🛡️ Gestión de Clan", "Administra tu imperio.", "open_clans"));
        inventory.setItem(33, createButton(Material.WITHER_SKELETON_SKULL, "&#E6CCFF🌑 Mercado Negro", "Artefactos prohibidos.", "open_blackmarket"));
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo anti-robo

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        ItemMeta meta = event.getCurrentItem().getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "hub_action");

        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            player.closeInventory();

            // Ejecutamos el comando asociado al botón con 3 ticks de retraso para Bedrock
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                switch (action) {
                    case "open_skills": player.performCommand("skills"); break;
                    case "open_colecciones": player.performCommand("colecciones"); break;
                    case "open_recipes": player.performCommand("recipes"); break;
                    case "open_bazar": player.performCommand("bazar"); break;
                    case "open_slayer": player.performCommand("slayer"); break;
                    case "open_pv": player.performCommand("pv"); break;
                    case "open_wardrobe": player.performCommand("wardrobe"); break;
                    case "open_fast_travel": player.performCommand("warp"); break;
                    case "open_clans": player.performCommand("clan"); break;
                    case "open_blackmarket": player.performCommand("mercadonegro"); break;
                }
            }, 3L);
        }
    }

    private ItemStack createPlayerProfile() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(player);
        meta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff<bold>TUS ESTADÍSTICAS</bold>"));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFOperario: &#00f5ff" + player.getName()));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000❤ Vida: &#E6CCFF100/100"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff🛡️ Defensa: &#E6CCFF25"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#8b0000⚔️ Fuerza: &#E6CCFF10"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff⚡ Velocidad: &#E6CCFF100%"));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#ff00ff🪙 Monedas: &#E6CCFF0.0"));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff💎 Gemas: &#E6CCFF0"));

        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createButton(Material mat, String name, String desc, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(CrossplayUtils.parseCrossplay(player, "<bold>" + name + "</bold>"));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFF" + desc));
        lore.add(CrossplayUtils.parseCrossplay(player, " "));
        lore.add(CrossplayUtils.parseCrossplay(player, "&#00f5ff► Clic para acceder"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "hub_action"), PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }
}
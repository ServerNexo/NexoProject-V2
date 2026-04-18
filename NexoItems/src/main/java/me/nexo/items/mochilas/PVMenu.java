package me.nexo.items.mochilas;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.items.NexoItems;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎒 NexoItems - Menú de Bóvedas del Vacío / Player Vaults (Arquitectura Enterprise)
 * Rendimiento: Cero Streams, Llaves Cacheadas O(1) y Prevención de Desincronización.
 */
public class PVMenu extends NexoMenu {

    private final NexoItems plugin;

    // 🌟 OPTIMIZACIÓN O(1): Cacheamos las llaves para no instanciar objetos basura por cada ítem o clic
    private final NamespacedKey pvKey;
    private final NamespacedKey hubKey;

    public PVMenu(Player player, NexoItems plugin) {
        super(player);
        this.plugin = plugin;

        // Se instancian UNA SOLA VEZ cuando el jugador abre el menú
        this.pvKey = new NamespacedKey(plugin, "pv_action");
        this.hubKey = new NamespacedKey(plugin, "hub_action");
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX COLOR: Usamos el serializador nativo de Adventure en lugar de NexoColor
        return LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize("&#9933FF🧳 <bold>BÓVEDAS DEL VACÍO</bold>".replace("&#", "&x&"))
        );
    }

    @Override
    public int getSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // El cristal morado automático

        for (int i = 1; i <= 9; i++) {
            boolean tienePerm = (i == 1) || player.hasPermission("nexo.pv." + i);

            ItemStack pvItem = new ItemStack(tienePerm ? Material.ENDER_CHEST : Material.MINECART);
            ItemMeta meta = pvItem.getItemMeta();

            if (meta != null) {
                if (tienePerm) {
                    meta.displayName(CrossplayUtils.parseCrossplay(player, "&#00f5ff✨ <bold>BÓVEDA #" + i + "</bold>"));

                    // 🌟 OPTIMIZACIÓN RENDIMIENTO: Reemplazamos los Streams por ArrayList nativo (Mucho más rápido)
                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>(4);
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFHaz clic para abrir tu"));
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFalmacenamiento dimensional."));
                    lore.add(net.kyori.adventure.text.Component.empty());
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#55FF55► Clic para acceder"));
                    meta.lore(lore);

                    // 🌟 Usamos la llave cacheada de la RAM
                    meta.getPersistentDataContainer().set(pvKey, PersistentDataType.INTEGER, i);

                } else {
                    meta.displayName(CrossplayUtils.parseCrossplay(player, "&#FF5555🔒 <bold>BÓVEDA #" + i + " BLOQUEADA</bold>"));

                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>(4);
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFNo tienes acceso a esta"));
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#E6CCFFbóveda todavía."));
                    lore.add(net.kyori.adventure.text.Component.empty());
                    lore.add(CrossplayUtils.parseCrossplay(player, "&#FF3366[!] Requiere rango o expansión."));
                    meta.lore(lore);
                }
                pvItem.setItemMeta(meta);
            }

            // Los ponemos en la fila central
            inventory.setItem(i + 8, pvItem);
        }

        // Botón de Volver al Hub
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#FF3366⬅ <bold>VOLVER AL HUB</bold>"));
            // 🌟 Llave Cacheada
            backMeta.getPersistentDataContainer().set(hubKey, PersistentDataType.STRING, "open_hub");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(getSlots() - 5, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // 🛑 Bloqueo anti-robos en el selector

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        // 🌟 BÚSQUEDA O(1) CON LLAVES EN RAM (Cero creación de basura)

        // Si dio clic a una Mochila Desbloqueada
        if (meta.getPersistentDataContainer().has(pvKey, PersistentDataType.INTEGER)) {
            int mochilaId = meta.getPersistentDataContainer().get(pvKey, PersistentDataType.INTEGER);

            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.0f);
            player.closeInventory();

            // 🛡️ Abre la mochila forzando el comando con retraso anti-bug de Bedrock (Evita Dupes de inventario doble)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.performCommand("pv " + mochilaId);
            }, 3L);
        }

        // Si dio clic en Volver
        else if (meta.getPersistentDataContainer().has(hubKey, PersistentDataType.STRING)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                new me.nexo.core.hub.HubMenu(player, me.nexo.core.NexoCore.getPlugin(me.nexo.core.NexoCore.class)).open();
            }, 3L);
        }
    }
}
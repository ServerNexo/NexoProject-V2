package me.nexo.items.mochilas;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.items.NexoItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 🎒 NexoItems - Menú de Bóvedas del Vacío / Player Vaults (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Region Scheduler, Dependencias Transitivas y Cero Estáticos.
 * Nota: Instanciado dinámicamente por jugador. NO usa @Singleton.
 */
public class PVMenu extends NexoMenu {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final NexoCore corePlugin;
    private final CrossplayUtils crossplayUtils;

    // 🌟 OPTIMIZACIÓN O(1): Cacheamos las llaves para no instanciar objetos basura por cada ítem o clic
    private final NamespacedKey pvKey;
    private final NamespacedKey hubKey;

    public PVMenu(Player player, NexoItems plugin, NexoCore corePlugin, CrossplayUtils crossplayUtils) {
        super(player, crossplayUtils); // 🌟 FIX ERROR SUPER: Pasamos CrossplayUtils a la superclase
        this.plugin = plugin;
        this.corePlugin = corePlugin;
        this.crossplayUtils = crossplayUtils;

        // Se instancian UNA SOLA VEZ cuando el jugador abre el menú
        this.pvKey = new NamespacedKey(plugin, "pv_action");
        this.hubKey = new NamespacedKey(plugin, "hub_action");
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Retornamos el String puro. El Core se encarga del parseo a Component
        return "&#9933FF🧳 <bold>BÓVEDAS DEL VACÍO</bold>";
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

            var pvItem = new ItemStack(tienePerm ? Material.ENDER_CHEST : Material.MINECART);

            // 🌟 PAPER NATIVE: editMeta es más seguro y no crea basura innecesaria en memoria
            final int vaultId = i;
            pvItem.editMeta(meta -> {
                if (tienePerm) {
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff✨ <bold>BÓVEDA #" + vaultId + "</bold>"));

                    // 🌟 OPTIMIZACIÓN JAVA 21: List.of() crea listas inmutables ultra rápidas en memoria
                    meta.lore(List.of(
                            crossplayUtils.parseCrossplay(player, "&#E6CCFFHaz clic para abrir tu"),
                            crossplayUtils.parseCrossplay(player, "&#E6CCFFalmacenamiento dimensional."),
                            net.kyori.adventure.text.Component.empty(),
                            crossplayUtils.parseCrossplay(player, "&#55FF55► Clic para acceder")
                    ));

                    // 🌟 Usamos la llave cacheada de la RAM
                    meta.getPersistentDataContainer().set(pvKey, PersistentDataType.INTEGER, vaultId);

                } else {
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555🔒 <bold>BÓVEDA #" + vaultId + " BLOQUEADA</bold>"));

                    meta.lore(List.of(
                            crossplayUtils.parseCrossplay(player, "&#E6CCFFNo tienes acceso a esta"),
                            crossplayUtils.parseCrossplay(player, "&#E6CCFFbóveda todavía."),
                            net.kyori.adventure.text.Component.empty(),
                            crossplayUtils.parseCrossplay(player, "&#FF3366[!] Requiere rango o expansión.")
                    ));
                }
            });

            // Los ponemos en la fila central
            inventory.setItem(i + 8, pvItem);
        }

        // Botón de Volver al Hub
        var back = new ItemStack(Material.ARROW);
        back.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF3366⬅ <bold>VOLVER AL HUB</bold>"));
            // 🌟 Llave Cacheada
            meta.getPersistentDataContainer().set(hubKey, PersistentDataType.STRING, "open_hub");
        });
        inventory.setItem(getSlots() - 5, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // 🛑 Bloqueo anti-robos en el selector

        var item = event.getCurrentItem();
        // 🌟 FIX GHOST ITEMS: Añadimos isEmpty() nativo de Paper
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();

        // 🌟 BÚSQUEDA O(1) CON LLAVES EN RAM (Cero creación de basura)

        // Si dio clic a una Mochila Desbloqueada
        if (meta.getPersistentDataContainer().has(pvKey, PersistentDataType.INTEGER)) {
            int mochilaId = meta.getPersistentDataContainer().get(pvKey, PersistentDataType.INTEGER);

            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.0f);
            player.closeInventory();

            // 🛡️ FOLIA SYNC: Abre la mochila o ejecuta el comando desde el Thread de la Región
            player.getScheduler().runDelayed(plugin, task -> {
                player.performCommand("pv " + mochilaId);
            }, null, 3L);
        }

        // Si dio clic en Volver
        else if (meta.getPersistentDataContainer().has(hubKey, PersistentDataType.STRING)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            player.closeInventory();

            // 🛡️ FOLIA SYNC: Inyectamos CrossplayUtils al llamar al HubMenu
            player.getScheduler().runDelayed(plugin, task -> {
                new me.nexo.core.hub.HubMenu(player, corePlugin, crossplayUtils).open();
            }, null, 3L);
        }
    }
}
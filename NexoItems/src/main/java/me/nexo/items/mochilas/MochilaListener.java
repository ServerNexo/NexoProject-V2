package me.nexo.items.mochilas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 🎒 NexoItems - Listener de Mochilas (Arquitectura Enterprise)
 * Rendimiento: Cero String Allocations (Uso de Tags O(1)), Prevención de Dupe Vector (Off-hand).
 */
@Singleton
public class MochilaListener implements Listener {

    private final MochilaManager manager;

    @Inject
    public MochilaListener(MochilaManager manager) {
        this.manager = manager;
    }

    // ===============================================
    // 🛡️ LÓGICA DE PROTECCIÓN AL CERRAR Y GUARDADO
    // ===============================================
    @EventHandler(priority = EventPriority.HIGH)
    public void alCerrarMochila(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MochilaManager.MochilaHolder holder) {
            Player p = (Player) event.getPlayer();
            int id = holder.getMochilaId();

            manager.guardarMochila(p, id, event.getInventory());

            // 🌟 FIX INMERSIÓN: Adiós al spam en el chat. Usamos Action Bar y Sonido O(1).
            CrossplayUtils.sendActionBar(p, "&#55FF55[✓] Bóveda #" + id + " sincronizada en la nube.");
            p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 0.5f, 1.0f);
        }
    }

    // ===============================================
    // 🛡️ BLOQUEO DE CONTENEDORES ANIDADOS (ANTI-DUPE)
    // ===============================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void alHacerClic(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MochilaManager.MochilaHolder)) return;

        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        Player p = (Player) event.getWhoClicked();

        // 1. Shift+Clic desde el inventario del jugador hacia la mochila
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInv.equals(event.getView().getBottomInventory())) {
            if (esMochilaProhibida(event.getCurrentItem())) {
                bloquearInteraccion(event, p);
            }
            return;
        }

        // 2. Clic directo o intercambios si el clic es DENTRO de la mochila
        if (clickedInv.equals(topInv)) {
            // Si intenta soltar un Shulker que tiene agarrado en el cursor
            if (esMochilaProhibida(event.getCursor())) {
                bloquearInteraccion(event, p);
                return;
            }

            // 3. 🛑 FIX DUPE VECTOR: Cubrimos Teclas 1-9 (Hotbar) Y el cambio de mano 'F' (Off-Hand)
            if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND) {

                ItemStack swapItem = (event.getClick() == ClickType.SWAP_OFFHAND) ?
                        p.getInventory().getItemInOffHand() :
                        p.getInventory().getItem(event.getHotbarButton());

                if (esMochilaProhibida(swapItem)) {
                    bloquearInteraccion(event, p);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void alArrastrar(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MochilaManager.MochilaHolder)) return;

        // Si arrastra un Shulker, bloqueamos si toca CUALQUIER slot de la mochila
        if (esMochilaProhibida(event.getOldCursor())) {
            int topSize = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    bloquearInteraccion(event, (Player) event.getWhoClicked());
                    return;
                }
            }
        }
    }

    // ===============================================
    // ⚡ UTILIDADES ENTERPRISE O(1)
    // ===============================================

    /**
     * 🌟 ZERO-GARBAGE: Verifica usando Tags nativos de Bukkit en vez de crear Strings (item.getType().name())
     */
    private boolean esMochilaProhibida(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return Tag.SHULKER_BOXES.isTagged(item.getType());
    }

    /**
     * 🌟 CENTRALIZACIÓN: Mismo castigo visual para cualquier infracción
     */
    private void bloquearInteraccion(org.bukkit.event.Cancellable event, Player p) {
        event.setCancelled(true);
        CrossplayUtils.sendActionBar(p, "&#FF5555[!] Anomalía Dimensional: No puedes guardar contenedores anidados.");
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }
}
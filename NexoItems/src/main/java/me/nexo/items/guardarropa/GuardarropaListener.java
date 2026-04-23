package me.nexo.items.guardarropa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 🎒 NexoItems - Interfaz del Sistema de Guardarropa (Arquitectura Enterprise Java 21)
 * Rendimiento: EditMeta, Inyección de Dependencias, Folia Region Sync y Switch Expressions.
 */
@Singleton
public class GuardarropaListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final GuardarropaManager manager;
    private final CrossplayUtils crossplayUtils;

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class GuardarropaMenuHolder implements InventoryHolder {
        private final Inventory inventory;
        public GuardarropaMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public GuardarropaListener(NexoItems plugin, GuardarropaManager manager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.manager = manager;
        this.crossplayUtils = crossplayUtils;
    }

    public void abrirMenu(Player p) {
        // 🌟 Título Seguro a Component
        var tituloFormat = crossplayUtils.parseCrossplay(p, "&#ff00ff👔 <bold>GUARDARROPA</bold>");
        var holder = new GuardarropaMenuHolder(tituloFormat);
        var inv = holder.getInventory();

        int[] slotsPresets = {11, 13, 15};
        int presetNum = 1;

        for (int slot : slotsPresets) {
            var soporte = new ItemStack(Material.ARMOR_STAND);
            final int currentPreset = presetNum; // Variable effectively final para la lambda
            
            // 🌟 PAPER NATIVE: Edición rápida sin generar basura en memoria
            soporte.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(p, "&#00f5ff✨ <bold>PRESET " + currentPreset + "</bold>"));

                // 🌟 JAVA 21: List.of() para inmutabilidad y rendimiento
                meta.lore(List.of(
                        crossplayUtils.parseCrossplay(p, "&#E6CCFFGuarda o equipa conjuntos"),
                        crossplayUtils.parseCrossplay(p, "&#E6CCFFcompletos de armadura."),
                        net.kyori.adventure.text.Component.empty(),
                        crossplayUtils.parseCrossplay(p, "&#55FF55► Clic Izquierdo para Equipar"),
                        crossplayUtils.parseCrossplay(p, "&#FFAA00► Clic Derecho para Guardar Actual")
                ));
            });
            
            inv.setItem(slot, soporte);
            presetNum++;
        }

        // 🛡️ FOLIA SYNC: La apertura del inventario debe ser en la Región del Jugador
        p.getScheduler().run(plugin, task -> {
            p.openInventory(inv);
            p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1.2f);
        }, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof GuardarropaMenuHolder)) return;

        event.setCancelled(true); // Bloquea robos de ítems visuales

        if (!(event.getWhoClicked() instanceof Player p)) return;
        int slot = event.getRawSlot();

        if (slot >= 27) return;

        // 🌟 JAVA 21: Switch Expression (Adiós a los if-else anidados)
        int presetId = switch (slot) {
            case 11 -> 1;
            case 13 -> 2;
            case 15 -> 3;
            default -> -1;
        };

        if (presetId != -1) {
            // El manager ya gestiona sus propios Hilos Virtuales
            if (event.isRightClick()) {
                manager.guardarPreset(p, presetId);
            } else if (event.isLeftClick()) {
                manager.equiparPreset(p, presetId);
            }
        }
    }
}
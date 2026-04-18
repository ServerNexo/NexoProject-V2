package me.nexo.items.guardarropa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.NexoItems;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🎒 NexoItems - Interfaz del Sistema de Guardarropa (Arquitectura Enterprise)
 */
@Singleton
public class GuardarropaListener implements Listener {

    private final NexoItems plugin;
    private final GuardarropaManager manager;

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class GuardarropaMenuHolder implements InventoryHolder {
        private final Inventory inventory;
        public GuardarropaMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public GuardarropaListener(NexoItems plugin, GuardarropaManager manager) {
        this.plugin = plugin;
        this.manager = manager; // Inyectado directamente, más rápido y limpio
    }

    public void abrirMenu(Player p) {
        // 🌟 Título Seguro a Component
        net.kyori.adventure.text.Component tituloFormat = CrossplayUtils.parseCrossplay(p, "&#ff00ff👔 <bold>GUARDARROPA</bold>");
        GuardarropaMenuHolder holder = new GuardarropaMenuHolder(tituloFormat);
        Inventory inv = holder.getInventory();

        int[] slotsPresets = {11, 13, 15};
        int presetNum = 1;

        for (int slot : slotsPresets) {
            ItemStack soporte = new ItemStack(Material.ARMOR_STAND);
            ItemMeta meta = soporte.getItemMeta();
            if (meta != null) {
                // 🌟 FIX: Textos directos sin getMessage
                meta.displayName(CrossplayUtils.parseCrossplay(p, "&#00f5ff✨ <bold>PRESET " + presetNum + "</bold>"));

                List<String> loreRaw = List.of(
                        "&#E6CCFFGuarda o equipa conjuntos",
                        "&#E6CCFFcompletos de armadura.",
                        "",
                        "&#55FF55► Clic Izquierdo para Equipar",
                        "&#FFAA00► Clic Derecho para Guardar Actual"
                );

                // 🌟 FIX: Usamos directamente CrossplayUtils.parseCrossplay que devuelve un Component
                meta.lore(loreRaw.stream()
                        .map(line -> CrossplayUtils.parseCrossplay(p, line))
                        .collect(Collectors.toList()));

                soporte.setItemMeta(meta);
            }
            inv.setItem(slot, soporte);
            presetNum++;
        }

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable (Adiós validación por títulos)
        if (!(event.getInventory().getHolder() instanceof GuardarropaMenuHolder)) return;

        event.setCancelled(true); // Bloquea robos de ítems visuales

        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot >= 27) return;

        int presetId = -1;
        if (slot == 11) presetId = 1;
        else if (slot == 13) presetId = 2;
        else if (slot == 15) presetId = 3;

        if (presetId != -1) {
            if (event.isRightClick()) {
                manager.guardarPreset(p, presetId);
            } else if (event.isLeftClick()) {
                manager.equiparPreset(p, presetId);
            }
        }
    }
}
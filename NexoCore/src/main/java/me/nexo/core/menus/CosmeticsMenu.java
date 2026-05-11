package me.nexo.core.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 🎨 Menú Global de Cosméticos (Hub Dinámico AAA)
 * Se construye automáticamente leyendo el CosmeticsHubRegistry.
 * Toda la lógica de colores de chat ha sido delegada al módulo correspondiente.
 */
public class CosmeticsMenu extends NexoMenu {

    private final CosmeticsHubRegistry registry;

    // 💉 INYECCIÓN: Solo pedimos las herramientas base y el Registro
    public CosmeticsMenu(Player player, CrossplayUtils crossplayUtils, CosmeticsHubRegistry registry) {
        super(player, crossplayUtils);
        this.registry = registry;
    }

    @Override
    public String getMenuName() {
        return "<dark_gray>🎨 Armario de Cosméticos</dark_gray>";
    }

    @Override
    public int getSlots() {
        return 27; // Reducido a 27 porque solo mostrará botones principales
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🌟 MAGIA: Dibujamos automáticamente todos los botones que los plugins registraron
        for (CosmeticsHubRegistry.HubButton button : registry.getAllButtons()) {
            setItem(button.slot(), button.material(), button.name(), button.lore());
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        // 🌟 MAGIA: Buscamos si el slot clickeado tiene una acción asignada y la ejecutamos
        CosmeticsHubRegistry.HubButton clickedButton = registry.getButton(e.getSlot());

        if (clickedButton != null) {
            clickedButton.onClick().accept(player); // Esto abrirá el sub-menú correspondiente
        }
    }
}
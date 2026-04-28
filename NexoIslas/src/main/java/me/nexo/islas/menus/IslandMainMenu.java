package me.nexo.islas.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * 🖼️ Menú Principal de la Isla (Adaptado a NexoMenu)
 */
public class IslandMainMenu extends NexoMenu {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandProfile profile;

    public IslandMainMenu(Player player, CrossplayUtils crossplayUtils, NexoIslas plugin, IslandManager islandManager, IslandProfile profile) {
        super(player, crossplayUtils); // 💉 PILAR 1: Pasamos las dependencias a la clase padre
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.profile = profile;
    }

    @Override
    public String getMenuName() {
        return "§8🏝️ Tu Imperio Celestial";
    }

    @Override
    public int getSlots() {
        return 27; // 3 Filas
    }

    @Override
    public void setMenuItems() {
        // Relleno automático usando tu método nativo del Core
        setFillerGlass();

        // 📈 Botón Izquierdo: Estadísticas (Slot 11)
        setItem(11, Material.SUNFLOWER, "§e§lEstadísticas del Nexo", List.of(
                "§7Nivel de Frontera: §b" + profile.getBorderLevel(),
                "",
                "§7Top Actividad (XP): §d" + profile.getActivityScore(),
                "§7Top Riqueza (Banco): §6$" + profile.getWealthScore(),
                "",
                "§e💡 §oSube de nivel farmeando",
                "§e   §oy depositando cristales."
        ));

        // 🚶 Botón Central: Viajar a la Isla (Slot 13)
        setItem(13, Material.BEACON, "§a§lViajar a la Isla", List.of(
                "§7Haz clic para materializarte",
                "§7en el centro de tu isla."
        ));

        // 👥 Botón Derecho: Gestión de Miembros (Slot 15)
        setItem(15, Material.PLAYER_HEAD, "§b§lGestión de Miembros", List.of(
                "§7Miembros actuales: §f" + profile.getMembers().size() + "§8/§7" + profile.getMemberLimit(),
                "",
                "§eHaz clic para invitar o",
                "§eexpulsar jugadores."
        ));
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        // Cancelamos para que el jugador no pueda robarse los ítems del menú
        e.setCancelled(true);

        // Validamos si hizo clic en su propio inventario en lugar del menú
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        // Ejecutamos acciones según el slot
        switch (e.getSlot()) {
            case 13: // Viajar a la isla
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                islandManager.loadIslandAsync(player);
                break;

            case 15: // Miembros
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                // 🌟 ABRIMOS EL MENÚ DE MIEMBROS
                new IslandMembersMenu(player, crossplayUtils, plugin, islandManager, profile).open();
                break;
        }
    }
}
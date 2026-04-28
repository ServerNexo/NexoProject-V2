package me.nexo.islas.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

/**
 * 👥 Menú de Gestión de Miembros de la Isla
 */
public class IslandMembersMenu extends NexoMenu {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandProfile profile;

    public IslandMembersMenu(Player player, CrossplayUtils crossplayUtils, NexoIslas plugin, IslandManager islandManager, IslandProfile profile) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.profile = profile;
    }

    @Override
    public String getMenuName() {
        return "§8👥 Gestión de Miembros";
    }

    @Override
    public int getSlots() {
        return 36; // 4 Filas
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 👑 1. Mostrar al Dueño (Siempre en el Slot 10)
        setPlayerHead(10, profile.getOwnerId(), "§6§l👑 Dueño de la Isla", List.of("§7El líder absoluto del Nexo."));

        // 👥 2. Mostrar a los Miembros actuales (A partir del Slot 11)
        int currentSlot = 11;
        for (UUID memberId : profile.getMembers()) {
            setPlayerHead(currentSlot, memberId, "§b§l👤 Miembro", List.of(
                    "§7Tiene permisos para construir",
                    "§7y acceder a los cofres.",
                    "",
                    "§c[Clic para expulsar]"
            ));
            currentSlot++;
        }

        // 🪑 3. Mostrar los espacios vacíos (Hasta llegar al límite)
        int emptySlots = profile.getMemberLimit() - profile.getMembers().size();
        for (int i = 0; i < emptySlots; i++) {
            setItem(currentSlot, Material.WHITE_STAINED_GLASS_PANE, "§f§lEspacio Disponible", List.of(
                    "§7Tienes espacio para invitar",
                    "§7a otro jugador a tu isla.",
                    "",
                    "§e💡 Usa: §b/is invite <jugador>"
            ));
            currentSlot++;
        }

        // 🔙 4. Botón de Regresar (Slot 31)
        setItem(31, Material.ARROW, "§c§l⬅ Regresar", List.of("§7Volver al panel principal."));
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        // 🔙 Lógica para regresar
        if (e.getSlot() == 31) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new IslandMainMenu(player, crossplayUtils, plugin, islandManager, profile).open();
            return;
        }

        // 🥾 Lógica para expulsar a un miembro (Si el que hace clic es el dueño)
        if (e.getSlot() >= 11 && e.getSlot() < 11 + profile.getMembers().size()) {
            if (!profile.getOwnerId().equals(player.getUniqueId())) {
                player.sendMessage("§c❌ Solo el dueño de la isla puede expulsar miembros.");
                return;
            }

            int memberIndex = e.getSlot() - 11;
            UUID targetId = profile.getMembers().get(memberIndex);
            
            // Aquí llamaríamos a un comando o método para expulsarlo
            player.closeInventory();
            player.performCommand("is kick " + Bukkit.getOfflinePlayer(targetId).getName());
        }
    }

    // 🛠️ Método especial para crear cabezas de jugadores
    private void setPlayerHead(int slot, UUID uuid, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(p);
            meta.setDisplayName(name + " §8- §f" + (p.getName() != null ? p.getName() : "Desconocido"));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        inventory.setItem(slot, head);
    }
}
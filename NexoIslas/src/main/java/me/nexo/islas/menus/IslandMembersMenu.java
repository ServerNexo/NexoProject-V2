package me.nexo.islas.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 👥 NexoIslas - Menú de Gestión de Equipo (Arquitectura Enterprise)
 * Rendimiento: Interfaz unificada O(1), Cabezas Cacheadas y Layout AAA.
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
        return "&#00f5ff👥 &#55FF55Gestión de Miembros";
    }

    @Override
    public int getSlots() {
        return 54; // 🌟 Consistencia con el Menú Principal (6 Filas)
    }

    @Override
    public void setMenuItems() {
        // 🔲 FONDO INMERSIVO
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        bg.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) inventory.setItem(i, bg);

        // 👑 1. MOSTRAR AL DUEÑO (Centro Arriba - Slot 13)
        setPlayerHead(13, profile.getOwnerId(), "&#FFAA00<bold>👑 Dueño de la Isla</bold>", List.of(
                "&#AAAAAAEl líder absoluto del Nexo.",
                "&#AAAAAAPosee control total sobre el territorio."
        ));

        // 🌟 CALCULAMOS SLOTS DINÁMICAMENTE PARA CENTRARLOS (Fila 4 y 5)
        int[] memberSlots = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int maxMembers = profile.getRealMemberLimit(); // Límite actual comprado
        List<UUID> currentMembers = profile.getMembers();

        // 👥 2. MOSTRAR A LOS MIEMBROS ACTUALES
        for (int i = 0; i < maxMembers; i++) {
            int slot = memberSlots[i];

            if (i < currentMembers.size()) {
                // Hay un miembro ocupando este espacio
                UUID memberId = currentMembers.get(i);
                setPlayerHead(slot, memberId, "&#00f5ff<bold>👤 Miembro del Equipo</bold>", List.of(
                        "&#E6CCFFTiene permisos para construir",
                        "&#E6CCFFy acceder a los cofres.",
                        "",
                        "&#FF5555[!] Clic para expulsar"
                ));
            } else {
                // 🪑 3. ESPACIO VACÍO DISPONIBLE
                ItemStack empty = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
                empty.editMeta(meta -> {
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55<bold>Espacio Disponible</bold>"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAATienes espacio para invitar"));
                    lore.add(crossplayUtils.parseCrossplay(player, "&#AAAAAAa otro jugador a tu isla."));
                    lore.add(Component.empty());
                    lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00💡 Usa: &#ff00ff/is invite <jugador>"));
                    meta.lore(lore);
                });
                inventory.setItem(slot, empty);
            }
        }

        // 🔙 4. BOTÓN DE REGRESAR (Slot 49)
        ItemStack back = new ItemStack(Material.RED_BED);
        back.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555<bold>⬅ Regresar al Menú Principal</bold>")));
        inventory.setItem(49, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        // 🔙 REGRESAR
        if (e.getSlot() == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new IslandMainMenu(player, crossplayUtils, plugin, islandManager, profile).open();
            return;
        }

        // 🥾 EXPULSAR MIEMBRO
        int[] memberSlots = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

        for (int i = 0; i < profile.getMembers().size(); i++) {
            if (e.getSlot() == memberSlots[i]) {

                // Validar que sea el dueño
                if (!profile.getOwnerId().equals(player.getUniqueId())) {
                    crossplayUtils.sendMessage(player, "&#FF5555[x] Solo el líder puede expulsar miembros.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                UUID targetId = profile.getMembers().get(i);
                String targetName = Bukkit.getOfflinePlayer(targetId).getName();

                // Ejecutamos el comando de expulsión
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
                player.performCommand("is kick " + (targetName != null ? targetName : targetId.toString()));
                break;
            }
        }
    }

    // ==========================================
    // 🎨 MOTOR DE RENDERIZADO DE CABEZAS
    // ==========================================
    private void setPlayerHead(int slot, UUID uuid, String hexTitle, List<String> hexLore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        head.editMeta(meta -> {
            if (meta instanceof SkullMeta skull) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                skull.setOwningPlayer(p);

                String playerName = p.getName() != null ? p.getName() : "Desconocido";
                skull.displayName(crossplayUtils.parseCrossplay(player, hexTitle + " &#888888- &#FFFFFF" + playerName));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                for (String line : hexLore) {
                    lore.add(crossplayUtils.parseCrossplay(player, line));
                }
                skull.lore(lore);
            }
        });
        inventory.setItem(slot, head);
    }
}
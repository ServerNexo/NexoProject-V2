package me.aeroxis.islas.menus;

import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.islas.AeroxisIslas;
import me.aeroxis.islas.data.IslandProfile;
import me.aeroxis.islas.managers.IslandManager;
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
 * 👥 AeroxisIslas - Menú de Gestión de Equipo (Arquitectura Enterprise)
 * Rendimiento: Interfaz unificada O(1), HashMap seguro, y Layout AAA.
 */
public class IslandMembersMenu extends AeroxisMenu {

    private final AeroxisIslas plugin;
    private final IslandManager islandManager;
    private final IslandProfile profile;

    // 🌟 Mapeo rápido de UUIDs a sus slots para la función de expulsión
    private final List<UUID> renderizedMembers;

    public IslandMembersMenu(Player player, CrossplayUtils crossplayUtils, AeroxisIslas plugin, IslandManager islandManager, IslandProfile profile) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.profile = profile;
        this.renderizedMembers = new ArrayList<>();
    }

    @Override
    public String getMenuName() {
        return "&#00f5ff👥 &#55FF55Gestión de Equipo";
    }

    @Override
    public int getSlots() {
        return 54;
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

        // 🌟 CÁLCULOS DINÁMICOS
        int[] memberSlots = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int maxMembers = profile.getRealMemberLimit(); // Límite actual comprado

        // Extraemos los miembros del HashMap
        List<UUID> currentMembers = new ArrayList<>(profile.getMembers().keySet());

        // 👥 2. RENDERIZAR A LOS MIEMBROS ACTUALES
        for (int i = 0; i < maxMembers; i++) {
            int slot = memberSlots[i];

            if (i < currentMembers.size()) {
                // Renderizar a un miembro
                UUID memberId = currentMembers.get(i);
                renderizedMembers.add(memberId); // Lo guardamos en orden para poder expulsarlo

                String roleName = profile.getRole(memberId).name(); // Ej: "MEMBER", "MODERATOR"

                setPlayerHead(slot, memberId, "&#00f5ff<bold>👤 Integrante</bold>", List.of(
                        "&#E6CCFFRol Actual: &#55FF55" + roleName,
                        "&#E6CCFFPermisos de acceso estándar.",
                        "",
                        "&#FF5555[!] Clic para expulsar"
                ));
            } else {
                // 🪑 3. RENDERIZAR ESPACIO VACÍO DISPONIBLE
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

        // 🔙 4. BOTÓN DE REGRESAR
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

        // Iteramos solo hasta la cantidad de miembros que hemos renderizado
        for (int i = 0; i < renderizedMembers.size(); i++) {
            if (e.getSlot() == memberSlots[i]) {

                // Validar que sea el dueño
                if (!profile.getOwnerId().equals(player.getUniqueId())) {
                    crossplayUtils.sendMessage(player, "&#FF5555[x] Solo el líder puede expulsar miembros.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                UUID targetId = renderizedMembers.get(i);
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
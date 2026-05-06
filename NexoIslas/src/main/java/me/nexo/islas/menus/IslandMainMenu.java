package me.nexo.islas.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.data.IslandRole;
import me.nexo.islas.managers.IslandManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏝️ NexoIslas - Menú Principal de la Isla (Arquitectura Enterprise)
 * Rendimiento: Diseño 54 slots AAA, editMeta O(1) y Renombre Anti-Bugs Bedrock.
 */
public class IslandMainMenu extends NexoMenu {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandProfile profile;

    public IslandMainMenu(Player player, CrossplayUtils crossplayUtils, NexoIslas plugin, IslandManager islandManager, IslandProfile profile) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.profile = profile;
    }

    @Override
    public String getMenuName() {
        return "&#55FF55🏝 &#FFAA00Tu Imperio Celestial";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        // 🔲 FONDO INMERSIVO NEGRO
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        bg.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < getSlots(); i++) {
            inventory.setItem(i, bg);
        }

        // 💎 SLOT 13: MEJORAS DE ISLA (Upgrades)
        ItemStack upgrades = new ItemStack(Material.END_CRYSTAL);
        upgrades.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#00f5ff<bold>✧ Mejoras de Isla</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFExpande los límites, aumenta la generación"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFy mejora la productividad de tu isla."));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55Nivel de Frontera: &#FFAA00" + profile.getBorderLevel()));
            lore.add(crossplayUtils.parseCrossplay(player, "&#55FF55Puntos de Mejora: &#FFAA00" + profile.getUpgradePoints()));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para abrir árbol de mejoras"));
            meta.lore(lore);
        });
        inventory.setItem(13, upgrades);

        // 👥 SLOT 21: MIEMBROS
        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        members.editMeta(meta -> {
            if (meta instanceof SkullMeta skull) skull.setOwningPlayer(player);
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>👥 Gestión de Equipo</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFMiembros actuales: &#55FF55" + profile.getMembers().size() + " &#888888/ &#FFAA00" + profile.getMemberLimit()));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para administrar roles y miembros"));
            meta.lore(lore);
        });
        inventory.setItem(21, members);

        // 🌍 SLOT 23: VIAJAR A LA ISLA
        ItemStack teleport = new ItemStack(Material.BEACON);
        teleport.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55<bold>🌍 Viajar a la Isla</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFRegresa al núcleo de tu territorio."));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para teletransportarte"));
            meta.lore(lore);
        });
        inventory.setItem(23, teleport);

        // ⚙️ SLOT 25: CONFIGURACIÓN
        ItemStack settings = new ItemStack(Material.COMPARATOR);
        settings.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#AAAAAA<bold>⚙ Configuración</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFModifica el clima, ciclo de día,"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFpermisos de visitantes y más."));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para abrir ajustes"));
            meta.lore(lore);
        });
        inventory.setItem(25, settings);

        // 📈 SLOT 31: ESTADÍSTICAS DEL NEXO (FIX)
        ItemStack stats = new ItemStack(Material.SUNFLOWER);
        stats.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00<bold>📈 Estadísticas del Nexo</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFNivel de Isla: &#ff00ff" + profile.getLevel()));
            // 🌟 FIX APLICADO: Ahora usa getTotalXp()
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFExperiencia Total: &#ff00ff" + String.format("%.1f", profile.getTotalXp())));
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFValor (Cristales): &#55FF55" + profile.getValue()));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00💡 Sube de nivel farmeando"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00   y depositando cristales."));
            meta.lore(lore);
        });
        inventory.setItem(31, stats);

        // 🏷️ SLOT 33: RENOMBRAR ISLA (UX Bedrock Friendly)
        ItemStack rename = new ItemStack(Material.NAME_TAG);
        rename.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55<bold>🏷️ Renombrar Isla</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFNombre actual: &#FFFFFF" + profile.getIslandName()));
            lore.add(Component.empty());
            if (profile.getRole(player.getUniqueId()) == IslandRole.OWNER) {
                lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para cambiar el nombre"));
            } else {
                lore.add(crossplayUtils.parseCrossplay(player, "&#FF5555[x] Solo el líder puede hacer esto"));
            }
            meta.lore(lore);
        });
        inventory.setItem(33, rename);

        // ❌ SLOT 49: CERRAR
        ItemStack close = new ItemStack(Material.BARRIER);
        close.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555<bold>Cerrar Menú</bold>")));
        inventory.setItem(49, close);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        switch (e.getSlot()) {
            case 13: // 💎 Mejoras
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new IslandUpgradesMenu(player, crossplayUtils, plugin, islandManager, profile).open();
                break;

            case 21: // 👥 Miembros
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new IslandMembersMenu(player, crossplayUtils, plugin, islandManager, profile).open();
                break;

            case 23: // 🌍 Viajar
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                islandManager.loadIslandAsync(player);
                break;

            case 25: // ⚙️ Configuración
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new IslandSettingsMenu(player, crossplayUtils, plugin, islandManager, profile).open();
                break;

            case 31: // 📈 Estadísticas
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
                player.performCommand("is top"); // Ataque directo al comando /is top
                player.closeInventory();
                break;

            case 33: // 🏷️ Renombrar
                if (profile.getRole(player.getUniqueId()) != IslandRole.OWNER) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    crossplayUtils.sendMessage(player, "&#FF5555[x] Solo el líder absoluto puede cambiar el nombre de la isla.");
                    return;
                }

                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                crossplayUtils.sendMessage(player, "");
                crossplayUtils.sendMessage(player, "&#55FF55🏷️ <bold>RENOMBRE DE ISLA</bold>");
                crossplayUtils.sendMessage(player, "&#FFAA00Escribe el nuevo nombre de tu isla en el chat.");
                crossplayUtils.sendMessage(player, "&#888888(O escribe 'cancelar' para abortar)");
                crossplayUtils.sendMessage(player, "");

                islandManager.addRenameSession(player.getUniqueId(), profile);
                break; // 🌟 FIX: ¡Agregado el break faltante!

            case 49: // ❌ Cerrar
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
                player.closeInventory();
                break;
        }
    }
}
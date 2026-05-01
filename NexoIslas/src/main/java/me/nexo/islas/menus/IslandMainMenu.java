package me.nexo.islas.menus;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
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
 * Rendimiento: Diseño 54 slots AAA, editMeta O(1) y Soporte Crossplay nativo.
 */
public class IslandMainMenu extends NexoMenu {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandProfile profile;

    // 💉 PILAR 1: Inyección de Dependencias
    public IslandMainMenu(Player player, CrossplayUtils crossplayUtils, NexoIslas plugin, IslandManager islandManager, IslandProfile profile) {
        super(player, crossplayUtils);
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.profile = profile;
    }

    @Override
    public String getMenuName() {
        // Formato Hexadecimal para Menús Inmersivos
        return "&#55FF55🏝 &#FFAA00Tu Imperio Celestial";
    }

    @Override
    public int getSlots() {
        return 54; // 6 Filas (Diseño AAA Completo)
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
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para abrir árbol de mejoras"));
            meta.lore(lore);
        });
        inventory.setItem(13, upgrades);

        // 👥 SLOT 21: MIEMBROS
        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        members.editMeta(meta -> {
            if (meta instanceof SkullMeta skull) skull.setOwningPlayer(player); // Cara del propio jugador
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff<bold>👥 Gestión de Equipo</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFMiembros actuales: &#55FF55" + profile.getMembers().size() + " &#888888/ &#FFAA00" + profile.getRealMemberLimit()));
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

        // 📈 SLOT 31: ESTADÍSTICAS DEL NEXO
        ItemStack stats = new ItemStack(Material.SUNFLOWER);
        stats.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00<bold>📈 Estadísticas del Nexo</bold>"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFTop Actividad (XP): &#ff00ff" + profile.getActivityScore()));
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFTop Riqueza (Banco): &#55FF55$" + String.format("%,.0f", profile.getWealthScore())));
            lore.add(Component.empty());
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFF💡 Sube de nivel farmeando"));
            lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFF   y depositando cristales."));
            meta.lore(lore);
        });
        inventory.setItem(31, stats);

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
                crossplayUtils.sendMessage(player, "&#FFAA00[!] Conectando con el panel de mejoras...");
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

            case 31: // 📈 Valor de Isla
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
                // Aquí en un futuro puedes abrir el NexoMenu de Tops/Rankings
                break;

            case 49: // ❌ Cerrar
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
                player.closeInventory();
                break;
        }
    }
}
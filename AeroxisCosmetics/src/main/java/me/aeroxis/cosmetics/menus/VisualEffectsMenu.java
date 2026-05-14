package me.aeroxis.cosmetics.menus;

import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.utils.SoundManager;
import me.aeroxis.cosmetics.manager.CosmeticManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * ✨ Menú de Efectos Visuales (Módulo AeroxisCosmetics)
 * Catálogo Masivo: 6 Alas, 3 Halos, 5 Estelas, 4 Fatalities.
 */
public class VisualEffectsMenu extends AeroxisMenu {

    private final UserManager userManager;
    private final SoundManager soundManager;
    private final CosmeticManager cosmeticManager;
    private final AeroxisUser user;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public VisualEffectsMenu(Player player, CrossplayUtils crossplayUtils, UserManager userManager, SoundManager soundManager, CosmeticManager cosmeticManager) {
        super(player, crossplayUtils);
        this.userManager = userManager;
        this.soundManager = soundManager;
        this.cosmeticManager = cosmeticManager;
        this.user = userManager.getUserOrNull(player.getUniqueId());
    }

    @Override public String getMenuName() { return "<dark_gray>✨ Efectos Visuales</dark_gray>"; }
    @Override public int getSlots() { return 54; }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        if (user == null) return;

        // FILA 1: 6 Alas de Partículas
        setItem(9, Material.FEATHER, "<aqua><bold>ALAS CELESTIALES</bold></aqua>", List.of("<gray>Rastros mágicos en tu espalda.</gray>"));
        renderItem(10, "wings_angel", Material.QUARTZ, "Alas de Ángel");
        renderItem(11, "wings_demon", Material.COAL, "Alas de Demonio");
        renderItem(12, "wings_fire", Material.BLAZE_POWDER, "Alas de Fénix");
        renderItem(13, "wings_fairy", Material.CHERRY_SAPLING, "Alas de Hada");
        renderItem(14, "wings_void", Material.END_PORTAL_FRAME, "Alas del Vacío");
        renderItem(15, "wings_frost", Material.SNOWBALL, "Alas de Escarcha");

        // FILA 2: 3 Halos
        setItem(18, Material.TOTEM_OF_UNDYING, "<gold><bold>HALOS ORBITALES</bold></gold>", List.of("<gray>Anillos luminosos en tu cabeza.</gray>"));
        renderItem(20, "halo_divine", Material.GOLD_NUGGET, "Halo Divino");
        renderItem(21, "halo_cursed", Material.SOUL_LANTERN, "Halo Maldito");
        renderItem(22, "halo_king", Material.GOLDEN_HELMET, "Halo del Rey");

        // FILA 3: 5 Estelas de Proyectil
        setItem(27, Material.ARROW, "<green><bold>ESTELAS DE FLECHA</bold></green>", List.of("<gray>Rastro al disparar proyectiles.</gray>"));
        renderItem(29, "trail_heart", Material.ROSE_BUSH, "Corazones");
        renderItem(30, "trail_omen", Material.OMINOUS_BOTTLE, "Presagio");
        renderItem(31, "trail_fire", Material.CAMPFIRE, "Fuego Ardiente");
        renderItem(32, "trail_smoke", Material.GUNPOWDER, "Humo Ninja");
        renderItem(33, "trail_music", Material.NOTE_BLOCK, "Notas Musicales");

        // FILA 4: 4 Efectos de Asesinato (Kill Effects)
        setItem(36, Material.WITHER_ROSE, "<red><bold>FATALITIES</bold></red>", List.of("<gray>Aniquila Jefes con estilo.</gray>"));
        renderItem(38, "kill_abduction", Material.BEACON, "Rayo de Abducción");
        renderItem(39, "kill_thunder", Material.LIGHTNING_ROD, "Ira de Zeus");
        renderItem(40, "kill_hemorrhage", Material.REDSTONE, "Hemorragia");
        renderItem(41, "kill_blackhole", Material.ENDER_EYE, "Agujero Negro");

        setItem(48, Material.DARK_OAK_DOOR, "<red><bold>Volver al Armario</bold></red>", List.of("<gray>Regresa al menú principal.</gray>"));
        setItem(50, Material.BARRIER, "<red><bold>Quitar Cosméticos</bold></red>", List.of("<gray>Desactiva todos tus efectos visuales.</gray>"));
    }

    private void renderItem(int slot, String id, Material mat, String name) {
        boolean unlocked = user.getUnlockedCosmetics().contains(id) || player.hasPermission("aeroxiscosmetics.all"); // 🌟 FIX
        CosmeticManager.ActiveCosmetics current = cosmeticManager.getActiveCosmetics(player.getUniqueId());

        // 🌟 Comprobar equipamiento incluyendo el Halo
        boolean isEquipped = current != null && (id.equals(current.wingsEffect()) || id.equals(current.projectileTrail()) || id.equals(current.killEffect()) || id.equals(current.haloEffect()));

        ItemStack item = new ItemStack(unlocked ? mat : Material.GRAY_DYE);
        item.editMeta(meta -> {
            meta.displayName(mm.deserialize("<!italic><yellow>" + name + "</yellow>"));
            if (unlocked) {
                if (isEquipped) {
                    meta.lore(List.of(mm.deserialize("<!italic><aqua>✓ Equipado (Clic para quitar)</aqua>")));
                } else {
                    meta.lore(List.of(mm.deserialize("<!italic><green>▶ Clic para Equipar</green>")));
                }
            } else {
                meta.lore(List.of(mm.deserialize("<!italic><red>🔒 Bloqueado</red>")));
            }
        });
        inventory.setItem(slot, item);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (user == null || e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        int slot = e.getSlot();

        if (slot == 48) { player.closeInventory(); player.performCommand("cosmeticos"); return; }

        // 🛑 Quitar todo (Pasamos los 7 nulos)
        if (slot == 50) {
            cosmeticManager.setActiveCosmetics(player.getUniqueId(), new CosmeticManager.ActiveCosmetics(null, null, null, null, null, null, null));
            soundManager.playReset(player);
            player.sendMessage(mm.deserialize("<red>🧹 Has limpiado todos tus efectos visuales.</red>"));
            player.closeInventory();
            return;
        }

        String selectedId = null;
        String type = null;

        // Mapeo preciso de slots
        if (slot >= 10 && slot <= 15) { type = "wings"; }
        if (slot == 10) selectedId = "wings_angel";
        if (slot == 11) selectedId = "wings_demon";
        if (slot == 12) selectedId = "wings_fire";
        if (slot == 13) selectedId = "wings_fairy";
        if (slot == 14) selectedId = "wings_void";
        if (slot == 15) selectedId = "wings_frost";

        if (slot >= 20 && slot <= 22) { type = "halo"; }
        if (slot == 20) selectedId = "halo_divine";
        if (slot == 21) selectedId = "halo_cursed";
        if (slot == 22) selectedId = "halo_king";

        if (slot >= 29 && slot <= 33) { type = "trail"; }
        if (slot == 29) selectedId = "trail_heart";
        if (slot == 30) selectedId = "trail_omen";
        if (slot == 31) selectedId = "trail_fire";
        if (slot == 32) selectedId = "trail_smoke";
        if (slot == 33) selectedId = "trail_music";

        if (slot >= 38 && slot <= 41) { type = "kill"; }
        if (slot == 38) selectedId = "kill_abduction";
        if (slot == 39) selectedId = "kill_thunder";
        if (slot == 40) selectedId = "kill_hemorrhage";
        if (slot == 41) selectedId = "kill_blackhole";

        if (selectedId != null && type != null) {
            if (!user.getUnlockedCosmetics().contains(selectedId) && !player.hasPermission("aeroxiscosmetics.all")) { // 🌟 FIX
                soundManager.playError(player);
                return;
            }

            CosmeticManager.ActiveCosmetics current = cosmeticManager.getActiveCosmetics(player.getUniqueId());
            if (current == null) current = new CosmeticManager.ActiveCosmetics(null, null, null, null, null, null, null);

            String finalWings = current.wingsEffect();
            String finalHalo = current.haloEffect();
            String finalTrail = current.projectileTrail();
            String finalKill = current.killEffect();
            boolean isUnequipping = false;

            // 🌟 SISTEMA TOGGLE
            if ("wings".equals(type)) {
                if (selectedId.equals(current.wingsEffect())) { finalWings = null; isUnequipping = true; } else { finalWings = selectedId; }
            } else if ("halo".equals(type)) {
                if (selectedId.equals(current.haloEffect())) { finalHalo = null; isUnequipping = true; } else { finalHalo = selectedId; }
            } else if ("trail".equals(type)) {
                if (selectedId.equals(current.projectileTrail())) { finalTrail = null; isUnequipping = true; } else { finalTrail = selectedId; }
            } else if ("kill".equals(type)) {
                if (selectedId.equals(current.killEffect())) { finalKill = null; isUnequipping = true; } else { finalKill = selectedId; }
            }

            // 🌟 REGISTRAMOS CON LOS 7 PARÁMETROS EXACTOS
            CosmeticManager.ActiveCosmetics updated = new CosmeticManager.ActiveCosmetics(
                    current.joinTag(), finalTrail, finalKill, finalWings, finalHalo, current.islandBorder(), current.islandPet()
            );

            cosmeticManager.setActiveCosmetics(player.getUniqueId(), updated);

            if (isUnequipping) {
                soundManager.playReset(player);
                player.sendMessage(mm.deserialize("<yellow>Cosmético desequipado.</yellow>"));
            } else {
                soundManager.playShaderEquip(player);
                player.sendMessage(mm.deserialize("<green>✨ Cosmético equipado exitosamente.</green>"));
            }
            player.closeInventory();
        }
    }
}
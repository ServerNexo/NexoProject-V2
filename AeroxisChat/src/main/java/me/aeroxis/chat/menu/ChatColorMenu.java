package me.aeroxis.chat.menu;

import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.utils.SoundManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 🎨 Menú de Colores de Chat (Módulo NexoChat)
 */
public class ChatColorMenu extends AeroxisMenu {

    private final UserManager userManager;
    private final SoundManager soundManager;
    private final AeroxisUser user;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ChatColorMenu(Player player, CrossplayUtils crossplayUtils, UserManager userManager, SoundManager soundManager) {
        super(player, crossplayUtils);
        this.userManager = userManager;
        this.soundManager = soundManager;
        this.user = userManager.getUserOrNull(player.getUniqueId());
    }

    @Override
    public String getMenuName() {
        return "<dark_gray>🎨 Identidad Visual</dark_gray>";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        if (user == null) return;

        // ==========================================
        // 🔮 VISTA PREVIA (Slot 4)
        // ==========================================
        String activeTag = user.getChatColor() != null ? user.getChatColor() : "<gray>";
        String nameString = activeTag + player.getName() + (activeTag.contains("<gradient") ? "</gradient>" : "");

        ItemStack previewItem = new ItemStack(Material.PLAYER_HEAD);
        previewItem.editMeta(meta -> {
            meta.displayName(mm.deserialize("<!italic><yellow>Vista Previa en Chat:</yellow>"));
            meta.lore(List.of(
                    Component.empty(),
                    mm.deserialize("<!italic><dark_gray> » </dark_gray>" + nameString),
                    Component.empty(),
                    mm.deserialize("<!italic><gray>(Los shaders se animan solo en el chat).</gray>")
            ));
        });
        inventory.setItem(4, previewItem);

        // ==========================================
        // 🌟 FILA 1: SHADER / SHINE (Animación)
        // ==========================================
        setItem(10, Material.NETHER_STAR, "<light_purple><bold>SHADERS (Shine)</bold></light_purple>", List.of("<gray>Destellos holográficos animados.</gray>"));
        renderCosmeticItem(12, "shine_hielo", Material.HEART_OF_THE_SEA, "<aqua>Shine Hielo</aqua>", "<#000100>");
        renderCosmeticItem(13, "shine_esmeralda", Material.SLIME_BALL, "<green>Shine Esmeralda</green>", "<#000001>");
        renderCosmeticItem(14, "shine_dorado", Material.BELL, "<yellow>Shine Dorado</yellow>", "<#010100>");
        renderCosmeticItem(15, "shine_vacio", Material.DRAGON_BREATH, "<dark_purple>Shine Vacío</dark_purple>", "<#010001>");
        renderCosmeticItem(16, "shine_blanco", Material.IRON_NUGGET, "<white>Shine Puro</white>", "<#000101>");

        // ==========================================
        // ✨ FILA 2: SHADER / RGB (Animación)
        // ==========================================
        setItem(19, Material.MAGMA_CREAM, "<gold><bold>SHADERS (RGB)</bold></gold>", List.of("<gray>Efectos dinámicos en movimiento.</gray>"));
        renderCosmeticItem(21, "rgb_fuego", Material.BLAZE_POWDER, "<red>Fuego RGB</red>", "<#010000>");
        renderCosmeticItem(22, "rgb_arcoiris", Material.GLOW_BERRIES, "<rainbow>Arcoíris RGB</rainbow>", "<#020000>");
        renderCosmeticItem(23, "rgb_toxico", Material.SPIDER_EYE, "<dark_green>Tóxico RGB</dark_green>", "<#030000>");
        renderCosmeticItem(24, "rgb_plasma", Material.AMETHYST_SHARD, "<light_purple>Plasma RGB</light_purple>", "<#020002>");
        renderCosmeticItem(25, "rgb_oceano", Material.NAUTILUS_SHELL, "<blue>Océano RGB</blue>", "<#000002>");

        // ==========================================
        // 🎨 FILA 3: GRADIENTES HEXA (Estáticos)
        // ==========================================
        setItem(28, Material.NAME_TAG, "<aqua><bold>GRADIENTES HEXA</bold></aqua>", List.of("<gray>Transiciones de color premium.</gray>"));
        renderCosmeticItem(30, "grad_sunset", Material.FIRE_CHARGE, "<gradient:#ff7e5f:#feb47b>Ocaso</gradient>", "<gradient:#ff7e5f:#feb47b>");
        renderCosmeticItem(31, "grad_ocean", Material.WATER_BUCKET, "<gradient:#2b5876:#4e4376>Profundo</gradient>", "<gradient:#2b5876:#4e4376>");
        renderCosmeticItem(32, "grad_candy", Material.SWEET_BERRIES, "<gradient:#ff9a9e:#fecfef>Pastel</gradient>", "<gradient:#ff9a9e:#fecfef>");
        renderCosmeticItem(33, "grad_neon", Material.GLOW_INK_SAC, "<gradient:#00f2fe:#4facfe>Neón</gradient>", "<gradient:#00f2fe:#4facfe>");
        renderCosmeticItem(34, "grad_dark", Material.COAL, "<gradient:#434343:#000000>Oscuro</gradient>", "<gradient:#434343:#000000>");

        // ==========================================
        // 🧱 FILA 4: COLORES SÓLIDOS (Vanilla)
        // ==========================================
        setItem(37, Material.PAINTING, "<green><bold>SÓLIDOS</bold></green>", List.of("<gray>Colores puros básicos.</gray>"));
        renderCosmeticItem(39, "solid_red", Material.RED_DYE, "<red>Rojo</red>", "<red>");
        renderCosmeticItem(40, "solid_blue", Material.BLUE_DYE, "<blue>Azul</blue>", "<blue>");
        renderCosmeticItem(41, "solid_green", Material.LIME_DYE, "<green>Verde</green>", "<green>");
        renderCosmeticItem(42, "solid_yellow", Material.YELLOW_DYE, "<yellow>Amarillo</yellow>", "<yellow>");
        renderCosmeticItem(43, "solid_white", Material.WHITE_DYE, "<white>Blanco</white>", "<white>");

        // ==========================================
        // ⬅️ VOLVER Y 🛑 RESET (Slots 48 y 50)
        // ==========================================
        setItem(48, Material.DARK_OAK_DOOR, "<red><bold>Volver al Armario</bold></red>", List.of("<gray>Regresa al menú principal.</gray>"));
        setItem(50, Material.BARRIER, "<red><bold>Restablecer Nombre</bold></red>", List.of("<gray>Vuelve a tu color gris normal.</gray>"));
    }

    private void renderCosmeticItem(int slot, String id, Material mat, String displayName, String colorTag) {
        boolean hasUnlocked = user.getUnlockedCosmetics().contains(id) || player.hasPermission("nexochat.rgb");

        ItemStack item = new ItemStack(hasUnlocked ? mat : Material.GRAY_DYE);
        item.editMeta(meta -> {
            if (hasUnlocked) {
                meta.displayName(mm.deserialize("<!italic>" + displayName));
                meta.lore(List.of(
                        mm.deserialize("<!italic><gray>Estado: <green>Desbloqueado</green></gray>"),
                        Component.empty(),
                        mm.deserialize("<!italic><yellow>▶ Haz clic para equipar este estilo.</yellow>")
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.displayName(mm.deserialize("<!italic><dark_gray>🔒 </dark_gray>" + displayName));
                meta.lore(List.of(
                        mm.deserialize("<!italic><gray>Estado: <red>Bloqueado</red></gray>"),
                        Component.empty(),
                        mm.deserialize("<!italic><red>❌ No posees este cosmético.</red>"),
                        mm.deserialize("<!italic><yellow>💡 Desbloquéalo en Cajas de Actividad/Premium.</yellow>")
                ));
            }
        });
        inventory.setItem(slot, item);
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (user == null || e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        int slot = e.getSlot();

        // ⬅️ Volver
        if (slot == 48) {
            player.closeInventory();
            player.performCommand("cosmeticos");
            return;
        }

        // 🧱 Reset
        if (slot == 50) {
            applyColor("<gray>", "reset");
            return;
        }

        // Mapeo Dinámico de Clics por Fila
        if (slot >= 12 && slot <= 16) handleCategoryClick(slot, "shader"); 
        else if (slot >= 21 && slot <= 25) handleCategoryClick(slot, "rgb_animado"); 
        else if (slot >= 30 && slot <= 34) handleCategoryClick(slot, "hex"); 
        else if (slot >= 39 && slot <= 43) handleCategoryClick(slot, "solid"); 
    }

    private void handleCategoryClick(int slot, String type) {
        if (type.equals("shader")) {
            if (slot == 12) trySelectCosmetic("shine_hielo", "<#000100>", type);
            if (slot == 13) trySelectCosmetic("shine_esmeralda", "<#000001>", type);
            if (slot == 14) trySelectCosmetic("shine_dorado", "<#010100>", type);
            if (slot == 15) trySelectCosmetic("shine_vacio", "<#010001>", type);
            if (slot == 16) trySelectCosmetic("shine_blanco", "<#000101>", type);
        } else if (type.equals("rgb_animado")) {
            if (slot == 21) trySelectCosmetic("rgb_fuego", "<#010000>", "shader");
            if (slot == 22) trySelectCosmetic("rgb_arcoiris", "<#020000>", "shader");
            if (slot == 23) trySelectCosmetic("rgb_toxico", "<#030000>", "shader");
            if (slot == 24) trySelectCosmetic("rgb_plasma", "<#020002>", "shader");
            if (slot == 25) trySelectCosmetic("rgb_oceano", "<#000002>", "shader");
        } else if (type.equals("hex")) {
            if (slot == 30) trySelectCosmetic("grad_sunset", "<gradient:#ff7e5f:#feb47b>", "gradient");
            if (slot == 31) trySelectCosmetic("grad_ocean", "<gradient:#2b5876:#4e4376>", "gradient");
            if (slot == 32) trySelectCosmetic("grad_candy", "<gradient:#ff9a9e:#fecfef>", "gradient");
            if (slot == 33) trySelectCosmetic("grad_neon", "<gradient:#00f2fe:#4facfe>", "gradient");
            if (slot == 34) trySelectCosmetic("grad_dark", "<gradient:#434343:#000000>", "gradient");
        } else if (type.equals("solid")) {
            if (slot == 39) trySelectCosmetic("solid_red", "<red>", type);
            if (slot == 40) trySelectCosmetic("solid_blue", "<blue>", type);
            if (slot == 41) trySelectCosmetic("solid_green", "<green>", type);
            if (slot == 42) trySelectCosmetic("solid_yellow", "<yellow>", type);
            if (slot == 43) trySelectCosmetic("solid_white", "<white>", type);
        }
    }

    private void trySelectCosmetic(String id, String colorTag, String audioType) {
        if (!user.getUnlockedCosmetics().contains(id) && !player.hasPermission("nexochat.rgb")) {
            soundManager.playError(player);
            return;
        }
        applyColor(colorTag, audioType);
    }

    private void applyColor(String colorTag, String audioType) {
        user.setChatColor(colorTag);
        userManager.saveUserAsync(user);

        switch (audioType) {
            case "shader" -> soundManager.playShaderEquip(player);
            case "gradient" -> soundManager.playGradientEquip(player);
            case "solid" -> soundManager.playSolidEquip(player);
            case "reset" -> soundManager.playReset(player);
        }

        player.sendMessage(mm.deserialize("\n<gradient:gold:yellow>✨ ¡Estilo actualizado!</gradient> " +
                "<gray>Tu mensaje ahora se verá así: <reset>" + colorTag + "¡Hola, soy Nexo!</reset>\n"));
        open();
    }
}
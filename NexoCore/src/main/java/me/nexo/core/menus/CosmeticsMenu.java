package me.nexo.core.menus;

import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 🎨 Menú Global de Cosméticos (Fusión de IdentityMenu + Sistema de Cajas)
 */
public class CosmeticsMenu extends NexoMenu {

    private final UserManager userManager;
    private final NexoUser user;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CosmeticsMenu(Player player, CrossplayUtils crossplayUtils, UserManager userManager) {
        super(player, crossplayUtils);
        this.userManager = userManager;
        // 🌟 FIX: Usamos getUserOrNull() porque devuelve directamente el NexoUser en lugar de un Optional
        this.user = userManager.getUserOrNull(player.getUniqueId());
    }

    @Override
    public String getMenuName() { return "§8🎨 Identidad Visual"; }

    @Override
    public int getSlots() { return 45; }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        if (user == null) return; // Protección por si el usuario aún no carga

        // ==========================================
        // 🔮 ÍTEM DE VISTA PREVIA (Rescatado de tu viejo menú)
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
                    mm.deserialize("<!italic><gray>(Nota: Las animaciones de Shader solo se</gray>"),
                    mm.deserialize("<!italic><gray>ven en el chat, no en este menú).</gray>")
            ));
        });
        inventory.setItem(4, previewItem); // Usamos setItem directo del inventario para mantener el Meta avanzado

        // ==========================================
        // 🔥 JERARQUÍA LEGENDARIA (Ahora por Desbloqueos de BD)
        // ==========================================
        // En lugar de usar permisos, ahora verifica si están en 'unlockedCosmetics' (ideal para Crates)
        renderCosmeticItem(19, "color_fuego", Material.BLAZE_POWDER, "<gradient:#ff4500:#ff8c00>Fuego</gradient>", "<#010000>");
        renderCosmeticItem(20, "color_hielo", Material.SNOWBALL, "<gradient:#00bfff:#87cefa>Hielo</gradient>", "<#000100>");
        renderCosmeticItem(21, "color_esmeralda", Material.EMERALD, "<gradient:#00ff00:#adff2f>Esmeralda</gradient>", "<#000001>");
        renderCosmeticItem(22, "color_dorado", Material.GOLD_INGOT, "<gradient:#ffd700:#ffae42>Dorado</gradient>", "<#010100>");
        renderCosmeticItem(23, "color_vacio", Material.ENDER_PEARL, "<gradient:#800080:#4b0082>Vacío</gradient>", "<#010001>");

        // ==========================================
        // ⚔️ JERARQUÍA ÉPICA (Info de Gradientes)
        // ==========================================
        ItemStack epicItem = new ItemStack(Material.NAME_TAG);
        epicItem.editMeta(meta -> {
            meta.displayName(mm.deserialize("<!italic><gradient:#FF5555:#FFAA00>✒ Nombre Épico (Gradiente)</gradient>"));
            meta.lore(List.of(
                    mm.deserialize("<!italic><gray>Requiere rango: <bold><color:#FF5555>ÉPICO</color></bold></gray>"),
                    Component.empty(),
                    mm.deserialize("<!italic><yellow>Click para ver instrucciones</yellow>")
            ));
        });
        inventory.setItem(25, epicItem);

        // ==========================================
        // 🧱 ESTÁNDAR (Reset)
        // ==========================================
        setItem(31, Material.BARRIER, "§c§lRestablecer Nombre", List.of("§7Vuelve a tu color gris normal."));
    }

    private void renderCosmeticItem(int slot, String id, Material mat, String displayName, String colorTag) {
        // Mantenemos retrocompatibilidad: Si tiene el permiso o si lo ganó en una caja
        boolean hasUnlocked = user.getUnlockedCosmetics().contains(id) || player.hasPermission("nexochat.rgb");

        if (hasUnlocked) {
            setItem(slot, mat, chatManagerFix(displayName) + " Legendario", List.of(
                    "§7Estado: §aDesbloqueado",
                    "",
                    "§e▶ Haz clic para equipar este estilo."
            ));
        } else {
            setItem(slot, Material.GRAY_DYE, "§8🔒 " + chatManagerFix(displayName), List.of(
                    "§7Estado: §cBloqueado",
                    "",
                    "§c❌ No posees este cosmético.",
                    "§e💡 Desbloquéalo en Cajas de Actividad/Premium."
            ));
        }
    }

    // Pequeño helper para parsear colores legacy sin depender del ChatManager aquí
    private String chatManagerFix(String text) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(mm.deserialize(text));
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (user == null || e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        int slot = e.getSlot();

        // 🧱 Reset
        if (slot == 31) {
            applyColor("<gray>");
            return;
        }

        // ⚔️ Info Épica
        if (slot == 25) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.sendMessage(mm.deserialize("<green>Para usar un gradiente escribe: <white>/chatcolor <#HEX1> <#HEX2></white></green>"));
            return;
        }

        // 🔥 Legendarios
        if (slot == 19) trySelectCosmetic("color_fuego", "<#010000>");
        if (slot == 20) trySelectCosmetic("color_hielo", "<#000100>");
        if (slot == 21) trySelectCosmetic("color_esmeralda", "<#000001>");
        if (slot == 22) trySelectCosmetic("color_dorado", "<#010100>");
        if (slot == 23) trySelectCosmetic("color_vacio", "<#010001>");
    }

    private void trySelectCosmetic(String id, String colorTag) {
        if (!user.getUnlockedCosmetics().contains(id) && !player.hasPermission("nexochat.rgb")) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        applyColor(colorTag);
    }

    private void applyColor(String colorTag) {
        user.setChatColor(colorTag);
        userManager.saveUserAsync(user);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        player.sendMessage(mm.deserialize("\n<gradient:gold:yellow>✨ ¡Estilo actualizado!</gradient> " +
                "<gray>Tu mensaje ahora se verá así: <reset>" + colorTag + "¡Hola, soy Nexo!</reset>\n"));

        open(); // Recargamos para actualizar la vista previa
    }
}
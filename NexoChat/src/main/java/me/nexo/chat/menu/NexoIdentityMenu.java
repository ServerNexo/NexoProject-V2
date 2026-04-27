package me.nexo.chat.menu;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.nexo.chat.managers.NexoChatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 🎨 NexoChat - Interfaz de Identidad Visual
 */
public class NexoIdentityMenu {

    private final NexoChatManager chatManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public NexoIdentityMenu(NexoChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public void open(Player player) {
        Gui gui = Gui.gui()
                .title(mm.deserialize("<bold><gradient:#FF5555:#FFAA00>Identidad Visual</gradient></bold>"))
                .rows(4)
                .create();

        gui.disableAllInteractions();

        // ==========================================
        // 🔮 ÍTEM DE VISTA PREVIA
        // ==========================================
        String activeTag = chatManager.getPlayerCosmetic(player.getUniqueId());

        // Si el jugador tiene un trigger de Shader (<#010000>), en el menú se verá casi negro.
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
        gui.setItem(4, new GuiItem(previewItem));

        // ==========================================
        // 🔥 JERARQUÍA LEGENDARIA (Nuevos Triggers AntiGravity)
        // ==========================================
        gui.setItem(19, createCosmeticItem(player, Material.BLAZE_POWDER, "Fuego", "<gradient:#ff4500:#ff8c00>", "<#010000>", "nexochat.rgb"));
        gui.setItem(20, createCosmeticItem(player, Material.SNOWBALL, "Hielo", "<gradient:#00bfff:#87cefa>", "<#000100>", "nexochat.rgb"));
        gui.setItem(21, createCosmeticItem(player, Material.EMERALD, "Esmeralda", "<gradient:#00ff00:#adff2f>", "<#000001>", "nexochat.rgb"));
        gui.setItem(22, createCosmeticItem(player, Material.GOLD_INGOT, "Dorado", "<gradient:#ffd700:#ffae42>", "<#010100>", "nexochat.rgb"));
        gui.setItem(23, createCosmeticItem(player, Material.ENDER_PEARL, "Vacío", "<gradient:#800080:#4b0082>", "<#010001>", "nexochat.rgb"));

        // ==========================================
        // ⚔️ JERARQUÍA ÉPICA
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
        gui.setItem(25, new GuiItem(epicItem, event -> {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.sendMessage(mm.deserialize("<green>Para usar un gradiente escribe: <white>/chatcolor <#HEX1> <#HEX2></white></green>"));
        }));

        // ==========================================
        // 🧱 ESTÁNDAR (Reset)
        // ==========================================
        ItemStack resetItem = new ItemStack(Material.BARRIER);
        resetItem.editMeta(meta -> {
            meta.displayName(mm.deserialize("<!italic><red>Restablecer Nombre</red>"));
            meta.lore(List.of(mm.deserialize("<!italic><gray>Vuelve a tu color normal.</gray>")));
        });
        gui.setItem(31, new GuiItem(resetItem, event -> {
            chatManager.setPlayerCosmetic(player.getUniqueId(), "<gray>");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
            open(player);
        }));

        gui.open(player);
    }

    private GuiItem createCosmeticItem(Player player, Material material, String name, String displayColor, String saveTag, String permission) {
        boolean hasPerm = player.hasPermission(permission);
        String statusColor = hasPerm ? "<green>" : "<red>";
        String statusText = hasPerm ? "Desbloqueado" : "Bloqueado";

        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(mm.deserialize("<!italic>" + displayColor + name + " Legendario"));
            meta.lore(List.of(
                    mm.deserialize("<!italic><gray>Estado: </gray>" + statusColor + statusText),
                    Component.empty(),
                    mm.deserialize(hasPerm ? "<!italic><yellow>Click para equipar</yellow>" : "<!italic><red>Requiere Rango Legendario</red>")
            ));
        });

        return new GuiItem(item, event -> {
            if (hasPerm) {
                chatManager.setPlayerCosmetic(player.getUniqueId(), saveTag);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                open(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        });
    }
}
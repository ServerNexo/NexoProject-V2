package me.nexo.chat.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.chat.database.NexoChatDatabase;
import me.nexo.chat.managers.NexoChatManager;
import me.nexo.chat.menu.TagsMenu;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.concurrent.CompletableFuture;

/**
 * 💬 Controlador de Comandos de Cosméticos de Chat
 */
@Singleton
@Command({"chat", "nexo"}) // Prefijos base
public class ComandoChat {

    private final NexoChatManager chatManager;
    private final NexoChatDatabase chatDatabase;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public ComandoChat(NexoChatManager chatManager, NexoChatDatabase chatDatabase, CrossplayUtils crossplayUtils) {
        this.chatManager = chatManager;
        this.chatDatabase = chatDatabase;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🏷️ MENÚ DE TAGS: /tags
    // ==========================================
    @Command({"tags", "titulos"})
    public void openTagsMenu(Player player) {
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        new TagsMenu(player, crossplayUtils, chatManager, chatDatabase).open();
    }

    // ==========================================
    // 🎁 DAR TAG (Admin / Consola)
    // ==========================================
    @Subcommand("tags give")
    @CommandPermission("nexochat.admin")
    public void giveTagToPlayer(Player admin, Player target, String tagId) {
        var config = Bukkit.getPluginManager().getPlugin("NexoChat").getConfig();

        if (!config.contains("tags_disponibles." + tagId)) {
            admin.sendMessage("§c❌ El tag '" + tagId + "' no existe en config.yml.");
            return;
        }

        // Lo añadimos a la RAM
        chatManager.getUnlockedTagsMap().putIfAbsent(target.getUniqueId(), new java.util.HashSet<>());
        chatManager.getUnlockedTagsMap().get(target.getUniqueId()).add(tagId);

        // 🌟 FIX: Guardado limpio (Ya no maneja colores, solo datos propios de Chat)
        CompletableFuture.runAsync(() -> {
            chatDatabase.savePlayerData(target.getUniqueId(), chatManager.getIgnoredPlayersMap().get(target.getUniqueId()));
        });

        admin.sendMessage("§a✅ Has otorgado el tag '" + tagId + "' a " + target.getName() + ".");
        target.sendMessage("§a🎉 ¡Has desbloqueado un nuevo título en el menú §b/tags§a!");
    }

    // ==========================================
    // 🎭 SOBRENOMBRES: /nick <nombre>
    // ==========================================
    @Command({"nick", "nickname"})
    @CommandPermission("nexochat.nick")
    public void setNickname(Player player, String newNick) {
        if (newNick.equalsIgnoreCase("off") || newNick.equalsIgnoreCase("remove")) {
            chatManager.setPlayerNickname(player.getUniqueId(), "");
            player.sendMessage("§c🗑️ Tu sobrenombre ha sido eliminado.");
        } else {
            // Límite de seguridad
            if (newNick.length() > 16) {
                player.sendMessage("§c❌ El sobrenombre no puede tener más de 16 caracteres.");
                return;
            }
            chatManager.setPlayerNickname(player.getUniqueId(), newNick);
            player.sendMessage("§a✨ Tu sobrenombre en el chat ahora es: §f*" + newNick);
        }

        // 🌟 FIX: Guardado limpio
        CompletableFuture.runAsync(() -> {
            chatDatabase.savePlayerData(player.getUniqueId(), chatManager.getIgnoredPlayersMap().get(player.getUniqueId()));
        });
    }

    // ==========================================
    // 🕵️ ANTI-FRAUDE: /realnick <sobrenombre>
    // ==========================================
    @Command("realnick")
    @CommandPermission("nexochat.admin")
    public void checkRealNick(Player admin, String fakeNick) {
        // Le quitamos el asterisco por si el admin lo escribió con él
        if (fakeNick.startsWith("*")) fakeNick = fakeNick.substring(1);

        String realName = chatManager.getRealNameFromNick(fakeNick);

        if (realName == null) {
            admin.sendMessage("§c❌ No hay ningún jugador conectado usando el apodo '" + fakeNick + "'.");
        } else {
            admin.sendMessage("§e🕵️ El jugador detrás del apodo §f*" + fakeNick + " §ees en realidad: §a§l" + realName);
        }
    }
}
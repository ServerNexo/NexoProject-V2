package me.nexo.core.crossplay;

import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class CrossplayUtils {

    public static void sendMessage(Player player, String rawMessage) {
        if (player != null) player.sendMessage(parseCrossplay(player, rawMessage));
    }

    public static void broadcastMessage(String rawMessage) {
        for (Player onlinePlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
            sendMessage(onlinePlayer, rawMessage);
        }
        org.bukkit.Bukkit.getConsoleSender().sendMessage(org.bukkit.ChatColor.stripColor(getChat(null, rawMessage)));
    }

    public static void sendActionBar(Player player, String rawMessage) {
        if (player != null) player.sendActionBar(parseCrossplay(player, rawMessage));
    }

    public static void sendTitle(Player player, String title, String subtitle) {
        if (player == null) return;
        Component parsedTitle = parseCrossplay(player, title);
        Component parsedSubtitle = parseCrossplay(player, subtitle);
        net.kyori.adventure.title.Title kyoriTitle = net.kyori.adventure.title.Title.title(parsedTitle, parsedSubtitle);
        player.showTitle(kyoriTitle);
    }

    public static String getChat(Player player, String rawMessage) {
        if (rawMessage == null) return "";
        Component component = parseCrossplay(player, rawMessage);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static Component parseCrossplay(Player player, String rawMessage) {
        if (rawMessage == null) rawMessage = "";

        if (player != null && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            rawMessage = rawMessage.replaceAll("[\\uE000-\\uF8FF]", "");
        }
        return NexoColor.parse(rawMessage);
    }

    public static int getOptimizedMenuSize(Player player, int targetSize) {
        if (player != null && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            if (targetSize > 36) {
                return 36;
            }
        }
        return targetSize;
    }
}
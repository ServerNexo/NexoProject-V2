package me.nexo.core.crossplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * 🏛️ Nexo Network - Utilidad de Crossplay (Arquitectura Enterprise)
 * Gestor de formato dinámico para clientes Java y Bedrock (Cero NMS, Cero Statics).
 */
@Singleton
public class CrossplayUtils {

    private final NexoColor nexoColor;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public CrossplayUtils(NexoColor nexoColor) {
        this.nexoColor = nexoColor;
    }

    public void sendMessage(Player player, String rawMessage) {
        if (player != null) {
            player.sendMessage(parseCrossplay(player, rawMessage));
        }
    }

    public void broadcastMessage(String rawMessage) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            sendMessage(onlinePlayer, rawMessage);
        }
        // 🌟 NATIVO PAPER: La consola acepta Componentes Kyori directamente (ANSI Colors)
        Bukkit.getConsoleSender().sendMessage(parseCrossplay(null, rawMessage));
    }

    public void sendActionBar(Player player, String rawMessage) {
        if (player != null) {
            player.sendActionBar(parseCrossplay(player, rawMessage));
        }
    }

    public void sendTitle(Player player, String title, String subtitle) {
        if (player == null) return;
        Component parsedTitle = parseCrossplay(player, title);
        Component parsedSubtitle = parseCrossplay(player, subtitle);
        net.kyori.adventure.title.Title kyoriTitle = net.kyori.adventure.title.Title.title(parsedTitle, parsedSubtitle);
        player.showTitle(kyoriTitle);
    }

    public String getChat(Player player, String rawMessage) {
        if (rawMessage == null) return "";
        Component component = parseCrossplay(player, rawMessage);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public Component parseCrossplay(Player player, String rawMessage) {
        if (rawMessage == null) rawMessage = "";

        // Llamada a API externa segura (Floodgate)
        if (player != null && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            // Elimina caracteres unicode de texturas personalizadas que rompen en Bedrock
            rawMessage = rawMessage.replaceAll("[\\uE000-\\uF8FF]", "");
        }
        
        // Uso de la instancia inyectada de NexoColor
        return nexoColor.parse(rawMessage);
    }

    public int getOptimizedMenuSize(Player player, int targetSize) {
        if (player != null && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            if (targetSize > 36) {
                return 36; // Límite seguro para interfaces en dispositivos móviles
            }
        }
        return targetSize;
    }
}
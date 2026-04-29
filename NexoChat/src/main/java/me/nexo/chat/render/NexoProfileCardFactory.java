package me.nexo.chat.render;

import com.google.inject.Singleton;
import me.nexo.core.user.NexoUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

@Singleton
public class NexoProfileCardFactory {

    private final MiniMessage mm = MiniMessage.miniMessage();

    /**
     * Construye una tarjeta de perfil inmersiva para el HoverEvent.
     */
    public Component createProfileCard(Player player, NexoUser user, String displayName) {
        if (user == null) return Component.text(player.getName());

        // 📊 Cálculos de la Barra de Progreso (Nivel Nexo)
        int currentLvl = user.getNexoNivel();
        int currentXp = user.getNexoXp();
        int requiredXp = currentLvl * 100;
        float progress = (float) currentXp / requiredXp;

        String progressBar = generateProgressBar(progress, 10, "<#00bfff>", "<dark_gray>");

        // 🛠️ Tarjeta de Perfil AAA (MiniMessage)
        String hoverFormat = 
                "<gradient:#FF5555:#FFAA00><bold>✦ PERFIL DE " + displayName.toUpperCase() + " ✦</bold></gradient>\n" +
                "<dark_gray><strikethrough>                              </strikethrough></dark_gray>\n" +
                "\n" +
                "<gray>Nivel Global:</gray> <yellow>" + currentLvl + " ✰</yellow>\n" +
                "<gray>XP:</gray> " + progressBar + " <white>(" + currentXp + "/" + requiredXp + ")</white>\n" +
                "\n" +
                "<gray>Salud:</gray> <red>" + (int) player.getHealth() + "❤</red> <dark_gray>|</dark_gray> <gray>Ping:</gray> <green>" + player.getPing() + "ms</green>\n" +
                "\n" +
                "<gray>Clan:</gray> " + (user.hasClan() ? "<aqua>[" + user.getClanRole() + "]</aqua>" : "<dark_gray>Ninguno</dark_gray>") + "\n" +
                "\n" +
                "<yellow>▶ Clic para enviar mensaje privado</yellow>";

        return mm.deserialize(hoverFormat);
    }

    /**
     * Genera una barra de progreso visual.
     * Ejemplo: ██████░░░░
     */
    private String generateProgressBar(float percentage, int length, String completeColor, String incompleteColor) {
        int completedBars = Math.round(percentage * length);
        int incompleteBars = length - completedBars;

        return completeColor + "█".repeat(Math.max(0, completedBars)) +
               incompleteColor + "█".repeat(Math.max(0, incompleteBars));
    }
}
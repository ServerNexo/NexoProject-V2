package me.aeroxis.chat.render;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.chat.ChatRenderer;
import me.aeroxis.core.user.AeroxisAPI;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.chat.AeroxisChatPlugin;
import me.aeroxis.chat.managers.AeroxisChatManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
public class AeroxisChatRenderer implements ChatRenderer {

    private final AeroxisChatPlugin plugin;
    private final AeroxisChatManager chatManager;
    private final AeroxisProfileCardFactory profileCardFactory;
    private final AeroxisAPI aeroxisApi;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public AeroxisChatRenderer(AeroxisChatPlugin plugin, AeroxisChatManager chatManager, AeroxisProfileCardFactory profileCardFactory, AeroxisAPI aeroxisApi) {
        this.plugin = plugin;
        this.chatManager = chatManager;
        this.profileCardFactory = profileCardFactory;
        this.aeroxisApi = aeroxisApi;
    }

    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
        AeroxisUser user = aeroxisApi.getUserLocal(source.getUniqueId());

        // 1. OBTENCIÓN DE DATOS BÁSICOS
        String realName = source.getName();
        String nickname = chatManager.getPlayerNickname(source.getUniqueId());
        String displayName = nickname.isEmpty() ? realName : "*" + nickname;

        // 2. LECTURA DE COSMÉTICOS (Color de Chat / Shaders)
        String cosmeticTag = (user != null && user.getChatColor() != null) ? user.getChatColor() : "<gray>";

        // Verificación estricta de permisos de Shaders (Prevención de inyección)
        boolean isLegendaryShader = cosmeticTag.matches("<#[0-9]{5}[0-9A-Fa-f]>") && cosmeticTag.contains("000"); // Lógica rápida para detectar el color mágico

        // 🌟 CORRECCIÓN: Se cambió "nexochat.rgb" a "aeroxischat.rgb"
        if (isLegendaryShader && !source.hasPermission("aeroxischat.rgb")) {
            cosmeticTag = "<gray>"; // Fallback de seguridad
            isLegendaryShader = false;
        }

        // 3. OBTENCIÓN DEL ÍCONO (TAG)
        String activeTagId = chatManager.getPlayerActiveTag(source.getUniqueId());
        String customIcon = "";

        if (!activeTagId.isEmpty()) {
            String configPath = "tags_disponibles." + activeTagId + ".icono";
            if (plugin.getConfig().contains(configPath)) {
                customIcon = plugin.getConfig().getString(configPath) + " ";
            }
        }

        // 4. OBTENCIÓN DEL PREFIJO LUCKPERMS (PlaceholderAPI)
        String lpPrefix = "";
        if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            lpPrefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(source, "%luckperms_prefix%");
        }

        // 5. ENVOLTURA MÁGICA (WRAPPING GLOBAL PARA SHADERS)
        String finalNameString;

        if (isLegendaryShader) {
            // 🌟 MAGIA AAA: Quitamos TODOS los colores originales del prefijo y del ícono.
            String plainPrefix = PlainTextComponentSerializer.plainText().serialize(chatManager.parseColors(lpPrefix));
            String plainIcon = PlainTextComponentSerializer.plainText().serialize(chatManager.parseColors(customIcon));

            // Envolvemos TODO (Prefijo + Icono + Nick) bajo el Color Mágico del Shader
            finalNameString = cosmeticTag + plainPrefix + plainIcon + displayName;
        } else {
            // Si NO es Shader (ej. color normal o gradiente), respetamos los colores nativos de LuckPerms y el Tag
            finalNameString = lpPrefix + customIcon + cosmeticTag + displayName + (cosmeticTag.contains("<gradient") ? "</gradient>" : "");
        }

        Component identityComponent = chatManager.parseColors(finalNameString);

        // 6. RICH TOOLTIP (Perfil de jugador)
        Component hoverCard = profileCardFactory.createProfileCard(source, user, displayName);
        identityComponent = identityComponent
                .hoverEvent(HoverEvent.showText(hoverCard))
                .clickEvent(ClickEvent.suggestCommand("/mensaje " + realName + " "));

        // 7. RENDERIZACIÓN INDEPENDIENTE DEL MENSAJE
        // Aquí el mensaje hereda su propio color (blanco por defecto), independientemente de si el Nick tiene un gradiente o shader.
        return Component.text()
                .append(identityComponent)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(message.colorIfAbsent(NamedTextColor.WHITE)) // Separación estricta de color
                .build();
    }
}
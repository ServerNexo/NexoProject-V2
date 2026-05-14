package me.aeroxis.chat.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * 🎥 AeroxisChat - Comando para Creadores de Contenido
 * Renderiza el logo de la plataforma usando texturas de Mojang.
 * Diseño AAA con Marcos de Gradiente y Filtro Anti-Shaders (Falso Negro).
 */
@Singleton
@Command("stream")
@CommandPermission("aeroxis.chat.stream") // 🌟 FIX: Actualizado a aeroxis
public class ComandoStream {

    private final CrossplayUtils crossplayUtils;

    @Inject
    public ComandoStream(CrossplayUtils crossplayUtils) {
        this.crossplayUtils = crossplayUtils;
    }

    public enum Plataforma {
        TWITCH, KICK, TIKTOK, YOUTUBE
    }

    @Subcommand("anunciar")
    public void anunciarStream(Player player, @Named("plataforma") Plataforma plataforma, @Named("link") String link) {

        String hexColor;
        String textureUrl;

        // 1. Asignamos el color HEX y la URL directa
        switch (plataforma) {
            case TWITCH:
                hexColor = "#9146FF"; // Morado Twitch
                textureUrl = "https://textures.minecraft.net/texture/46be65f44cd21014c8cddd0158bf75227adcb1fd179f4c1acd158c88871a13f";
                break;
            case KICK:
                hexColor = "#53FC18"; // Verde Kick
                textureUrl = "https://textures.minecraft.net/texture/74d52aa9fa704a5e21ee48ac0c764c4864a6dc649f4301873f5b9ca7029ca422";
                break;
            case TIKTOK:
                hexColor = "#00F2FE"; // Cyan TikTok
                textureUrl = "https://textures.minecraft.net/texture/58d02984a43e6c6910d0d908a57e041c3cfb1dd881b5b720c55563e681f59e0e";
                break;
            case YOUTUBE:
                hexColor = "#FF0000"; // Rojo YouTube
                textureUrl = "https://textures.minecraft.net/texture/fb95209d36c5aa1bf6c6f307f09b16d9058844ac560340c88f8394682ef57a0a";
                break;
            default:
                hexColor = "#FFFFFF"; // Blanco Neutro
                textureUrl = "https://textures.minecraft.net/texture/e36e94f6c34a35465fce4a90f2e25976389eb9709a12273574ff70fd4daa6852";
                break;
        }

        String colorPrincipal = "<" + hexColor + ">";

        // 2. Descargamos la textura engañando a Mojang con un User-Agent seguro
        BufferedImage skin = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(textureUrl).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            skin = ImageIO.read(connection.getInputStream());
        } catch (Exception ignored) {}

        // 3. DISEÑO AAA: Preparamos el texto a la derecha con Jerarquía Visual
        String[] lineasTexto = new String[8];
        for (int i = 0; i < 8; i++) lineasTexto[i] = "";

        // 🌟 FIX VISUAL: Cierres automáticos limpios de MiniMessage
        lineasTexto[1] = "   " + colorPrincipal + "<bold>● ¡TRANSMISIÓN EN DIRECTO! ●</bold>";
        lineasTexto[2] = "   <gray>¡<white>" + player.getName() + "</white> acaba de prender stream!</gray>";
        lineasTexto[3] = "   <gray>Acompáñalo ahora mismo en " + colorPrincipal + "<bold>" + plataforma.name() + "</bold><gray>.</gray>";
        lineasTexto[5] = "   <click:open_url:'" + link + "'><hover:show_text:'<gray>Click para abrir en tu navegador</gray>'>"
                + colorPrincipal + "<bold>▶ [ CLICK AQUÍ PARA ENTRAR ] ◀</bold></hover></click>";

        // =========================================================================
        // 🌟 MARCOS SEPARADORES (Efecto Visual de Brillo con Gradientes)
        // =========================================================================
        String lineaSuperior = "<gradient:" + hexColor + ":#222222><strikethrough>                                                                             </strikethrough></gradient>";
        String lineaInferior = "<gradient:#222222:" + hexColor + "><strikethrough>                                                                             </strikethrough></gradient>";

        // 4. Renderizamos los marcos, el pixel-art y el texto
        crossplayUtils.broadcastMessage("\n" + lineaSuperior + "\n");

        for (int y = 0; y < 8; y++) {
            StringBuilder lineBuilder = new StringBuilder();

            if (skin != null && skin.getWidth() >= 64) {
                for (int x = 0; x < 8; x++) {
                    int headPixel = skin.getRGB(8 + x, 8 + y);
                    int hatPixel = skin.getRGB(40 + x, 8 + y);

                    // Si el sombrero tiene color, lo usamos, si no, la cabeza base
                    int pixelColor = ((hatPixel >> 24) & 0xFF) > 0 ? hatPixel : headPixel;
                    int finalHex = pixelColor & 0xFFFFFF; // Limpiamos el canal Alpha

                    // Filtro Anti-Shaders para negros puros
                    if (finalHex == 0x000000) finalHex = 0x010101;

                    lineBuilder.append(String.format("<#%06X>█", finalHex));
                }
            } else {
                // Fallback hermoso si Mojang está caído (Sin el </color> roto)
                lineBuilder.append(colorPrincipal).append("████████");
            }

            // Unimos el logo pixel-art con su respectiva línea de texto a la derecha
            lineBuilder.append(lineasTexto[y]);
            crossplayUtils.broadcastMessage(lineBuilder.toString());
        }

        crossplayUtils.broadcastMessage("\n" + lineaInferior + "\n");
    }
}
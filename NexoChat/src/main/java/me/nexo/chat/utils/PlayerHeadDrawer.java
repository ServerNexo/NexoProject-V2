package me.nexo.chat.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import me.nexo.chat.managers.NexoChatManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PlayerHeadDrawer {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    /**
     * Dibuja la cara de 8x8 del jugador en el chat y le adjunta el texto del MOTD a la derecha.
     */
    public static List<Component> getFaceMotd(Player player, List<String> textLines, NexoChatManager chatManager) {
        List<Component> finalMotd = new ArrayList<>();
        BufferedImage skin = null;

        try {
            // Obtenemos la URL de la skin directamente de la sesión del jugador (API de Paper)
            URL skinUrl = player.getPlayerProfile().getTextures().getSkin();
            if (skinUrl != null) {
                skin = ImageIO.read(skinUrl);
            }
        } catch (Exception e) {
            // Si falla, el skin será null y pintaremos una cara gris por defecto
        }

        // La cara del jugador es de 8x8 píxeles. Dibujaremos 8 líneas.
        for (int y = 0; y < 8; y++) {
            StringBuilder lineBuilder = new StringBuilder();
            
            if (skin != null && skin.getWidth() >= 64) {
                // Leer píxel por píxel
                for (int x = 0; x < 8; x++) {
                    int headPixel = skin.getRGB(8 + x, 8 + y);
                    int hatPixel = skin.getRGB(40 + x, 8 + y); // La capa del sombrero/casco
                    
                    // Si el sombrero tiene opacidad (no es transparente), usamos el sombrero. Si no, la cabeza.
                    int pixelColor = ((hatPixel >> 24) & 0xFF) > 0 ? hatPixel : headPixel;
                    
                    // Convertir el RGB a formato HEX de MiniMessage <#RRGGBB>
                    String hex = String.format("<#%06X>", (0xFFFFFF & pixelColor));
                    lineBuilder.append(hex).append("█"); 
                }
            } else {
                // Cara de fallback si no tiene skin
                lineBuilder.append("<gray>████████</gray>");
            }

            lineBuilder.append("   "); // Espacio de separación entre la cara y el texto

            // Adjuntar la línea de texto correspondiente
            String text = (y < textLines.size()) ? textLines.get(y) : "";
            text = text.replace("%player%", player.getName());
            
            // Parseamos los colores del texto
            Component textComp = chatManager.parseColors(text);
            
            // Unimos la cara (con MiniMessage) y el texto (ya parseado)
            Component fullLine = mm.deserialize(lineBuilder.toString()).append(textComp);
            finalMotd.add(fullLine);
        }
        
        return finalMotd;
    }
}
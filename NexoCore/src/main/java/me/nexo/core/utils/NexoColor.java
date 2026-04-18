package me.nexo.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class NexoColor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    // Soporte para los viejos colores con '&' (ej: &a, &l)
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * Procesa texto con HEX (&#RRGGBB), Gradients (<gradient:#rojo:#azul>) y Legacy (&a).
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Convertir formato HEX anticuado (&#RRGGBB) al formato MiniMessage (<#RRGGBB>)
        text = text.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");

        // 2. Si el texto tiene Legacy (&), lo convertimos primero a Componente y luego lo pasamos por MiniMessage
        if (text.contains("&")) {
            Component legacyComp = LEGACY_SERIALIZER.deserialize(text);
            String intermediate = MINI_MESSAGE.serialize(legacyComp).replace("\\<", "<").replace("\\>", ">");
            return MINI_MESSAGE.deserialize(intermediate);
        }

        // 3. Procesamiento puro de MiniMessage
        return MINI_MESSAGE.deserialize(text);
    }
}
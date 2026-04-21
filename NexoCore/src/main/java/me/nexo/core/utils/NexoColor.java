package me.nexo.core.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * 🎨 NexoCore - Motor de Colores y Formato (Arquitectura Enterprise)
 * Convertido a un servicio inyectable (Singleton) para evitar llamadas estáticas y acoplamiento global.
 */
@Singleton
public class NexoColor {

    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public NexoColor() {
        this.miniMessage = MiniMessage.miniMessage();
        // Soporte para los viejos colores con '&' (ej: &a, &l)
        this.legacySerializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
    }

    /**
     * Procesa texto con HEX (&#RRGGBB), Gradients (<gradient:#rojo:#azul>) y Legacy (&a).
     * * @param text El texto crudo a procesar.
     * @return El Componente de Kyori Adventure listo para enviar al jugador.
     */
    public Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Convertir formato HEX anticuado (&#RRGGBB) al formato MiniMessage (<#RRGGBB>)
        text = text.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");

        // 2. Si el texto tiene Legacy (&), lo convertimos primero a Componente y luego lo pasamos por MiniMessage
        if (text.contains("&")) {
            Component legacyComp = legacySerializer.deserialize(text);
            String intermediate = miniMessage.serialize(legacyComp).replace("\\<", "<").replace("\\>", ">");
            return miniMessage.deserialize(intermediate);
        }

        // 3. Procesamiento puro de MiniMessage
        return miniMessage.deserialize(text);
    }
}
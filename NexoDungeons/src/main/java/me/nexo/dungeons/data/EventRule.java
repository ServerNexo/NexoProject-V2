package me.nexo.dungeons.data;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 🏰 NexoDungeons - Estructura de Datos de Eventos (Data Transfer Object)
 * Arquitectura: Java 21 Records (Inmutabilidad Estricta).
 * Nota: Esta clase es puramente de datos, no requiere @Singleton.
 */
public record EventRule(
        String id,
        boolean isInstanced,
        String worldName,
        Trigger trigger,
        List<Action> actions
) {
    // Constructor compacto para garantizar integridad de datos tras el parseo JSON
    public EventRule {
        actions = Objects.requireNonNullElse(actions, List.of());
        id = Objects.requireNonNullElse(id, "UNKNOWN_ID");
    }

    /**
     * El detonante (Ej: Hacer clic a un bloque en ciertas coordenadas)
     */
    public record Trigger(
            String type, 
            String material, 
            Map<String, Integer> loc
    ) {
        public Trigger {
            type = Objects.requireNonNullElse(type, "NONE");
            material = Objects.requireNonNullElse(material, "ANY");
            loc = Objects.requireNonNullElse(loc, Map.of());
        }
    }

    /**
     * Las acciones que se ejecutarán en cadena tras el trigger
     */
    public record Action(
            String type,
            String material,
            String sound,
            String itemId,
            String counterId,
            String mobId,
            Integer amount,
            Integer max,
            Double volume,
            Double pitch,
            Map<String, Integer> loc,
            Map<String, Integer> loc1,
            Map<String, Integer> loc2
    ) {
        // Garantizamos que los mapas no sean nulos para evitar errores en cálculos de offsets
        public Action {
            type = Objects.requireNonNullElse(type, "UNKNOWN");
            loc = Objects.requireNonNullElse(loc, Map.of());
            loc1 = Objects.requireNonNullElse(loc1, Map.of());
            loc2 = Objects.requireNonNullElse(loc2, Map.of());
        }
    }
}
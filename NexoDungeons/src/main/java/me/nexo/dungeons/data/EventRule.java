package me.nexo.dungeons.data;

import java.util.List;
import java.util.Map;

/**
 * Representa un evento/puzzle cargado desde el events.json
 */
public record EventRule(
        String id,
        boolean isInstanced,
        String worldName,
        Trigger trigger,
        List<Action> actions
) {
    // El detonante (Ej: Hacer clic a un bloque en ciertas coordenadas)
    public record Trigger(String type, String material, Map<String, Integer> loc) {}

    // Las acciones que se ejecutarán en cadena
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
    ) {}
}
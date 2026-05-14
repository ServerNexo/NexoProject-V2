package me.aeroxis.factories.core;

import java.util.Map;

/**
 * 🏭 AeroxisFactories - Data Record de Receta (Java 21)
 * Inmutable, ultra ligero y optimizado para búsquedas rápidas.
 */
public record CraftingRecipe(
        String id,
        String requiredNode,
        Map<String, Integer> ingredients,
        String resultItemId,
        int resultAmount
) {
    // 🌟 Lógica de validación O(1): Compara lo que hay en la mesa con esta receta
    public boolean matches(Map<String, Integer> itemsOnTable) {
        if (itemsOnTable.size() != ingredients.size()) return false;
        
        for (Map.Entry<String, Integer> req : ingredients.entrySet()) {
            if (itemsOnTable.getOrDefault(req.getKey(), 0) < req.getValue()) {
                return false;
            }
        }
        return true;
    }
}
package me.nexo.items.dtos;

import java.util.List;

/**
 * 🎒 NexoItems - DTO de Encantamientos (Arquitectura Enterprise Java 21)
 * Estructura inmutable, Null-Safe y lista para operaciones en Virtual Threads.
 */
public record EnchantDTO(
        String id,
        String nombre,
        int nivelMaximo,
        List<String> aplicaA,
        String descripcion,
        List<Double> valoresPorNivel
) {
    // 🛡️ Constructor Compacto: Garantiza Inmutabilidad Profunda y Null-Safety
    public EnchantDTO {
        aplicaA = (aplicaA == null) ? List.of() : List.copyOf(aplicaA);
        valoresPorNivel = (valoresPorNivel == null) ? List.of() : List.copyOf(valoresPorNivel);
    }

    /**
     * 🧠 Obtiene el valor matemático del encantamiento según su nivel actual.
     */
    public double getValorPorNivel(int nivel) {
        if (valoresPorNivel.isEmpty()) return 0.0;
        
        // Si piden un nivel mayor al máximo, les damos el último valor de la lista
        if (nivel > valoresPorNivel.size()) {
            return valoresPorNivel.get(valoresPorNivel.size() - 1);
        }
        
        // Si el nivel es 1, buscamos en el índice 0
        return valoresPorNivel.get(Math.max(0, nivel - 1));
    }

    /**
     * 🛡️ Verifica si el encantamiento es compatible con un tipo de ítem (Ej. "Arma", "Herramienta").
     */
    public boolean esCompatible(String tipoItem) {
        if (tipoItem == null) return false;
        
        // Evaluación declarativa rápida
        return aplicaA.stream()
                .anyMatch(t -> t.equalsIgnoreCase(tipoItem) || t.equalsIgnoreCase("Todos") || t.equalsIgnoreCase("Cualquiera"));
    }
}
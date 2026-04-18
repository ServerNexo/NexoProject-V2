package me.nexo.items.dtos;

import java.util.List;

public record EnchantDTO(
        String id,
        String nombre,
        int nivelMaximo,
        List<String> aplicaA,
        String descripcion,
        List<Double> valoresPorNivel
) {
    // Método rápido para obtener el valor del encantamiento según su nivel
    public double getValorPorNivel(int nivel) {
        if (valoresPorNivel == null || valoresPorNivel.isEmpty()) return 0.0;
        // Si piden un nivel mayor al máximo, les damos el último valor de la lista
        if (nivel > valoresPorNivel.size()) return valoresPorNivel.get(valoresPorNivel.size() - 1);
        // Si el nivel es 1, buscamos en el índice 0
        return valoresPorNivel.get(Math.max(0, nivel - 1));
    }

    public boolean esCompatible(String tipoItem) {
        if (tipoItem == null) return false;
        return aplicaA.stream().anyMatch(t -> t.equalsIgnoreCase(tipoItem) || t.equalsIgnoreCase("Todos"));
    }
}
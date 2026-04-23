package me.nexo.items.dtos;

import java.util.List;

/**
 * 🎒 NexoItems - DTO de Reforja / Alteración de Matriz (Arquitectura Enterprise Java 21)
 * Estructura inmutable, Thread-Safe y con evaluación declarativa.
 */
public record ReforgeDTO(
        String id,
        String nombre,
        String prefijoColor,
        List<String> clasesAplicables,
        int costoPolvo,
        double danioExtra,
        double velocidadAtaqueExtra,
        double fortunaExtra // ⬅️ ¡Aquí está la magia que faltaba!
) {
    
    // 🛡️ Constructor Compacto: Garantiza Inmutabilidad Profunda (Deep Immutability)
    public ReforgeDTO {
        clasesAplicables = List.copyOf(clasesAplicables); // Si le pasan un ArrayList mutable, lo sella.
    }

    /**
     * 🧠 Verifica si la alteración de matriz es compatible con la clase del jugador/arma.
     */
    public boolean aplicaAClase(String claseJugador) {
        if (claseJugador == null) return false;
        
        // 🌟 JAVA 21: Evaluación declarativa con Streams (Más limpio y optimizado)
        return clasesAplicables.stream()
                .anyMatch(clase -> clase.equalsIgnoreCase(claseJugador) || clase.equalsIgnoreCase("Cualquiera"));
    }
}
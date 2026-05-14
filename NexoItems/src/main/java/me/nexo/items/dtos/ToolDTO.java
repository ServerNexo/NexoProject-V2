package me.nexo.items.dtos;

/**
 * 🎒 NexoItems - DTO de Herramienta de Profesión (Arquitectura Enterprise Java 21)
 * Estructura de datos inmutable, Thread-Safe y con lógica de dominio encapsulada.
 */
public record ToolDTO(
        String id,
        String nombre,
        String rareza,
        String profesion,
        int tier,
        int nivelRequerido,
        double velocidadBase,
        double multiplicadorFortuna,
        String habilidadId
) {
    /**
     * ⛏️ Verifica si la herramienta cumple los requisitos para ser tratada como un Taladro (Minería Tier 3+).
     */
    public boolean esTaladro() {
        return profesion.equalsIgnoreCase("Minería") && tier >= 3;
    }

    /**
     * 🌱 Verifica si la herramienta tiene la capacidad de evolución matemática.
     */
    public boolean esEvolutiva() {
        return id.equalsIgnoreCase("azada_matematica");
    }

    // ==========================================
    // 🌌 LÓGICA DE LATE-GAME (AÑADIDO)
    // ==========================================

    /**
     * ☄️ Verifica si esta herramienta es el Gancho Espacial para NexoFracture.
     * Basado en la habilidad única en lugar del nombre visual.
     */
    public boolean isAnchorGrapple() {
        return "momentum_orbital".equalsIgnoreCase(habilidadId);
    }

    /**
     * 🌲 Verifica si esta herramienta es el Piolet para anclarse en Yggdrasil.
     */
    public boolean isAxePiolet() {
        return "escalada_vertical".equalsIgnoreCase(habilidadId);
    }
}
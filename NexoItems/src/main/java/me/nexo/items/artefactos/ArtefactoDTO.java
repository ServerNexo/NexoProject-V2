package me.nexo.items.artefactos;

/**
 * 🎒 NexoItems - DTO de Artefacto (Arquitectura Enterprise Java 21)
 * Estructura de datos inmutable, Thread-Safe y con enumeraciones encapsuladas.
 */
public record ArtefactoDTO(
        String id,
        String name,
        Rareza rarity,
        int cost,
        int cooldown,
        HabilidadType type,
        double power
) {
    /**
     * 🔮 Clasificación de rareza del artefacto y su color representativo.
     */
    public enum Rareza {
        COMUN("&#E6CCFF"),
        RARO("&#00f5ff"),
        EPICO("&#ff00ff"),
        LEGENDARIO("&#ff00ff"),
        MITICO("&#8b0000"),
        COSMICO("&#00f5ff");

        private final String color;

        Rareza(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * ⚡ Comportamiento de la habilidad del artefacto.
     */
    public enum HabilidadType {
        ACTIVA,      // Se ejecuta una vez y entra en Cooldown.
        TOGGLE,      // Se enciende y se apaga (ej. Alas), consume energía por segundo.
        DESPLIEGUE   // Invoca una entidad/marcador persistente.
    }
}
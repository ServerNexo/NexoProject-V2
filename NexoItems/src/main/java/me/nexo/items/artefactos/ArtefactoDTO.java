package me.nexo.items.artefactos;

public record ArtefactoDTO(
        String id,
        String name,
        Rareza rarity,
        int cost,
        int cooldown,
        HabilidadType type,
        double power
) {
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

    public enum HabilidadType {
        ACTIVA,
        TOGGLE,
        DESPLIEGUE
    }
}
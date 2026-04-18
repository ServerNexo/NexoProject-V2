package me.nexo.items.accesorios;

public record AccessoryDTO(
        String id,
        Familia family,
        Rareza rarity,
        StatType statType,
        double statValue,
        String abilityDescription
) {
    public enum Familia {
        MINERIA, TALA, COSECHA, PESCA, TANQUE, MELEE, RANGO, ENERGIA, MOVILIDAD, RIQUEZA, CAZAJEFES
    }

    public enum Rareza {
        COMUN(3, "&#E6CCFF"),
        RARO(8, "&#00f5ff"),
        EPICO(12, "&#ff00ff"),
        LEGENDARIO(16, "&#ff00ff"),
        MITICO(22, "&#8b0000"),
        COSMICO(30, "&#00f5ff");

        private final int poderNexo;
        private final String color;

        Rareza(int poderNexo, String color) {
            this.poderNexo = poderNexo;
            this.color = color;
        }
        public int getPoderNexo() { return poderNexo; }
        public String getColor() { return color; }
    }

    public enum StatType {
        FUERZA, VIDA, VELOCIDAD, ENERGIA_CUSTOM, ARMADURA
    }
}
package me.nexo.items.accesorios;

/**
 * 🎒 NexoItems - DTO de Accesorios (Arquitectura Enterprise Java 21)
 * Estructura inmutable, Thread-Safe y con lógica de dominio encapsulada.
 */
public record AccessoryDTO(
        String id,
        Familia family,
        Rareza rarity,
        StatType statType,
        double statValue,
        String abilityDescription
) {
    /**
     * 🧬 Categoría o linaje del accesorio.
     */
    public enum Familia {
        MINERIA, TALA, COSECHA, PESCA, TANQUE, MELEE, RANGO, ENERGIA, MOVILIDAD, RIQUEZA, CAZAJEFES
    }

    /**
     * 🔮 Clasificación de rareza, poder de nexo base y su color representativo.
     */
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
        
        public int getPoderNexo() { 
            return poderNexo; 
        }
        
        public String getColor() { 
            return color; 
        }
    }

    /**
     * 📊 Tipo de bonificación estadística que otorga el accesorio.
     */
    public enum StatType {
        FUERZA, VIDA, VELOCIDAD, ENERGIA_CUSTOM, ARMADURA
    }
}
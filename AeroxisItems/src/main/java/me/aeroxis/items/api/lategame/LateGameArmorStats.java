package me.aeroxis.items.api.lategame;

/**
 * 🌌 Estadísticas Avanzadas para Entornos Hostiles (Late-Game).
 * Calculadas por el ArmorListener y cacheadas en la sesión del jugador.
 */
public record LateGameArmorStats(
        double thrusterPower,     // Potencia de propulsión para NexoFracture (Gravedad Cero)
        double windResistance,    // Anulación de empuje para Yggdrasil Ascent (Tala Vertical)
        int thermalLevel,         // Resistencia climática para Chrono-Dome
        boolean hasSpecialPassive // Si posee inmunidad absoluta (Ej: Sneak-lock o Inmunidad al colapso)
) {
    /**
     * @return Un perfil vacío para jugadores sin armadura de Tier 8.
     */
    public static LateGameArmorStats empty() {
        return new LateGameArmorStats(0.0, 0.0, 0, false);
    }

    /**
     * @return TRUE si la armadura tiene capacidad de anclar al jugador contra vientos fuertes.
     */
    public boolean canResistWind() {
        return windResistance >= 1.0 || hasSpecialPassive;
    }
}
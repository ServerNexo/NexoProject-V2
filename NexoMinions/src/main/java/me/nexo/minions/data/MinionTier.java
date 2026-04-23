package me.nexo.minions.data;

/**
 * 🤖 NexoMinions - Calculadora de Tiers (Arquitectura Enterprise)
 * Rendimiento: Funciones Matemáticas Puras (Zero-Garbage, Stateless).
 * Nota: Al ser una Utility Class estática y sin estado, no requiere inyección de Guice.
 */
public final class MinionTier {

    // 🌟 OPTIMIZACIÓN O(1): Constante estática para evitar recrear el array en cada cálculo
    private static final int[] SLOTS_POR_NIVEL = {0, 1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 14, 15};

    // 🛡️ Previene la instanciación de esta clase de utilidades
    private MinionTier() {
        throw new UnsupportedOperationException("Esta es una clase de utilidades matemática y no puede ser instanciada.");
    }

    // Calcula los milisegundos que tarda en picar según el nivel
    public static long getDelayMillis(int tier) {
        // Tier 1 = 15.0s | Tier 12 = 6.0s
        double delaySegundos = 15.0 - ((tier - 1) * (9.0 / 11.0));
        return (long) (delaySegundos * 1000L);
    }

    // Calcula cuántos ítems le caben en la panza (1 slot = 64 ítems)
    public static int getMaxStorage(int tier) {
        // Tier 1 = 1 slot | Tier 12 = 15 slots
        int slots = (tier >= 1 && tier <= 12) ? SLOTS_POR_NIVEL[tier] : 1;
        return slots * 64;
    }
}
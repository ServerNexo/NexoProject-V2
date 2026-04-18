package me.nexo.minions.data;

public class MinionTier {

    // Calcula los milisegundos que tarda en picar según el nivel
    public static long getDelayMillis(int tier) {
        // Tier 1 = 15.0s | Tier 12 = 6.0s
        double delaySegundos = 15.0 - ((tier - 1) * (9.0 / 11.0));
        return (long) (delaySegundos * 1000L);
    }

    // Calcula cuántos ítems le caben en la panza (1 slot = 64 ítems)
    public static int getMaxStorage(int tier) {
        // Tier 1 = 1 slot | Tier 12 = 15 slots
        int[] slotsPorNivel = {0, 1, 2, 3, 4, 5, 6, 8, 10, 11, 12, 14, 15};
        int slots = (tier >= 1 && tier <= 12) ? slotsPorNivel[tier] : 1;
        return slots * 64;
    }
}
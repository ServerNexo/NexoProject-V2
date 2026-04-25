package me.nexo.minions.data;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;

/**
 * 🧬 NexoMinions - Genoma y Mutación (Data Component Inmutable)
 * Java 25 Record: Inmutable, thread-safe, y ultraligero en RAM.
 */
public record MinionDNA(
        @NotNull UUID ownerId,
        @NotNull MinionType type,
        int tier,
        double speedMutation,     // Multiplicador de velocidad base (1.0 = normal, 0.5 = 50% más rápido)
        double strikeProbability, // Probabilidad de irse a huelga (0.01 = 1%)
        double fatigueResistance, // Resistencia al cansancio
        int storedItems,
        long nextActionTime
) {
    // Factory method para un "Recién Nacido" sin mutaciones
    public static MinionDNA createBase(UUID ownerId, MinionType type, int tier) {
        return new MinionDNA(
                ownerId, type, tier, 
                1.0, 0.05, 1.0, // Stats estándar sin mutar
                0, System.currentTimeMillis() + 5000L
        );
    }
    
    // Método para crear una "copia mutada" (ya que los Records son inmutables)
    public MinionDNA withUpdatedState(int newStoredItems, long newNextActionTime) {
        return new MinionDNA(ownerId, type, tier, speedMutation, strikeProbability, fatigueResistance, newStoredItems, newNextActionTime);
    }
}
package me.aeroxis.minions.data;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 🧬 AeroxisMinions - Genoma y Mutación (Data Component Inmutable)
 * Java 21+ Record: Inmutable, thread-safe, ultraligero y Adaptable (Omni-Minion).
 */
public record MinionDNA(
        @NotNull UUID ownerId,
        @NotNull String currentProductionId, // 🌟 FASE 3: Reemplaza a MinionType (Ej: "DIAMOND_ORE")
        int tier,
        double speedMutation,     // Multiplicador de velocidad base (1.0 = normal, menor es más rápido)
        double strikeProbability, // Probabilidad de irse a huelga (0.05 = 5%, menor es mejor)
        double fatigueResistance, // Resistencia al cansancio (1.0 = normal, mayor es mejor)
        int storedItems,
        long nextActionTime
) {
    private static final Gson GSON = new Gson();

    // Factory method para un "Recién Nacido" sin mutaciones
    public static MinionDNA createBase(UUID ownerId, String currentProductionId, int tier) {
        return new MinionDNA(
                ownerId, currentProductionId, tier,
                1.0, 0.05, 1.0, // Stats estándar sin mutar
                0, System.currentTimeMillis() + 5000L
        );
    }

    // Método para crear una "copia actualizada" para el ciclo de procesamiento
    public MinionDNA withUpdatedState(int newStoredItems, long newNextActionTime) {
        return new MinionDNA(ownerId, currentProductionId, tier, speedMutation, strikeProbability, fatigueResistance, newStoredItems, newNextActionTime);
    }

    // 🌟 NUEVO FASE 3: Mutación Industrial (Cambiar de trabajo)
    public MinionDNA withUpdatedProduction(String newProductionId) {
        // Al cambiar de trabajo, se resetea el inventario para evitar trampas (ej. farmear piedra y cobrar como diamante)
        return new MinionDNA(ownerId, newProductionId, tier, speedMutation, strikeProbability, fatigueResistance, 0, System.currentTimeMillis() + 5000L);
    }

    /**
     * 🧬 Lógica de Crianza y Mutación Genética.
     */
    public MinionDNA breedWith(MinionDNA partner) {
        // 1. Validar compatibilidad usando el nuevo String
        if (!this.currentProductionId.equals(partner.currentProductionId())) {
            throw new IllegalArgumentException("No se pueden cruzar Minions de distintas industrias.");
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double mutationFactor = 0.9 + (random.nextDouble() * 0.2);

        double newSpeed = ((this.speedMutation + partner.speedMutation()) / 2.0) * mutationFactor;
        double newStrike = ((this.strikeProbability + partner.strikeProbability()) / 2.0) * mutationFactor;
        double newFatigue = ((this.fatigueResistance + partner.fatigueResistance()) / 2.0) * mutationFactor;

        newSpeed = Math.max(0.1, newSpeed);
        newStrike = Math.max(0.001, newStrike);
        newFatigue = Math.min(5.0, newFatigue);

        return new MinionDNA(
                this.ownerId,
                this.currentProductionId,
                this.tier,
                newSpeed,
                newStrike,
                newFatigue,
                0,
                System.currentTimeMillis() + 5000L
        );
    }

    // 🌟 Utilidades de Serialización (Si usas JSON en tu Custom PersistentDataType)
    public String toJson() { return GSON.toJson(this); }
    public static MinionDNA fromJson(String json) { return GSON.fromJson(json, MinionDNA.class); }
}
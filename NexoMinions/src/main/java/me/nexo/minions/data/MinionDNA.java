package me.nexo.minions.data;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 🧬 NexoMinions - Genoma y Mutación (Data Component Inmutable)
 * Java 25 Record: Inmutable, thread-safe, y ultraligero en RAM.
 */
public record MinionDNA(
        @NotNull UUID ownerId,
        @NotNull MinionType type,
        int tier,
        double speedMutation,     // Multiplicador de velocidad base (1.0 = normal, menor es más rápido)
        double strikeProbability, // Probabilidad de irse a huelga (0.05 = 5%, menor es mejor)
        double fatigueResistance, // Resistencia al cansancio (1.0 = normal, mayor es mejor)
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

    // Método para crear una "copia actualizada" para el ciclo de procesamiento
    public MinionDNA withUpdatedState(int newStoredItems, long newNextActionTime) {
        return new MinionDNA(ownerId, type, tier, speedMutation, strikeProbability, fatigueResistance, newStoredItems, newNextActionTime);
    }

    /**
     * 🧬 Lógica de Crianza y Mutación Genética.
     * Combina este ADN con el de una pareja para generar un nuevo Minion.
     *
     * @param partner El ADN del Minion con el que se va a cruzar.
     * @return Un nuevo MinionDNA con estadísticas heredadas y mutadas.
     */
    public MinionDNA breedWith(MinionDNA partner) {
        // 1. Validar compatibilidad
        if (this.type != partner.type()) {
            throw new IllegalArgumentException("No se pueden cruzar Minions de distintos tipos industriales.");
        }

        // ThreadLocalRandom es obligatorio en Java asíncrono para evitar cuellos de botella al generar números aleatorios
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 2. Calcular factor de mutación: Rango entre 0.9 (-10%) y 1.1 (+10%)
        double mutationFactor = 0.9 + (random.nextDouble() * 0.2);

        // 3. Promediar y aplicar la mutación a cada gen
        double newSpeed = ((this.speedMutation + partner.speedMutation()) / 2.0) * mutationFactor;
        double newStrike = ((this.strikeProbability + partner.strikeProbability()) / 2.0) * mutationFactor;

        // La resistencia a la fatiga funciona a la inversa (más alto es mejor),
        // por lo que si el mutationFactor es < 1, empeora, si es > 1, mejora.
        double newFatigue = ((this.fatigueResistance + partner.fatigueResistance()) / 2.0) * mutationFactor;

        // 4. Establecer límites (Hardcaps) para proteger la economía del servidor MMO
        newSpeed = Math.max(0.1, newSpeed);        // Límite máximo de velocidad: 10% del tiempo base
        newStrike = Math.max(0.001, newStrike);    // Límite mínimo de huelga: 0.1%
        newFatigue = Math.min(5.0, newFatigue);    // Límite máximo de resistencia: 5 veces lo normal

        // 5. Devolver el nuevo individuo
        // Nace con 0 ítems almacenados y hereda el tier del primer padre.
        return new MinionDNA(
                this.ownerId,
                this.type,
                this.tier,
                newSpeed,
                newStrike,
                newFatigue,
                0,
                System.currentTimeMillis() + 5000L
        );
    }
}
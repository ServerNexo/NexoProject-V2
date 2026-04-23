package me.nexo.war.core;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ Nexo Network - Contrato de Guerra (Modelo de Datos Java 21)
 * Diseñado para ser inmutable en su estructura y 100% atómico en sus registros.
 */
public record WarContract(
        UUID warId,
        UUID clanAtacante,
        UUID clanDefensor,
        BigDecimal apuestaMonedas,
        long startTime,
        WarStatus status,
        int killsAtacante,
        int killsDefensor,

        // 🌟 Cross-Play Slots: Balanceo de plataformas
        int maxJavaSlots,
        int maxBedrockSlots,
        Set<UUID> atacanteJava,
        Set<UUID> atacanteBedrock,
        Set<UUID> defensorJava,
        Set<UUID> defensorBedrock
) {
    /**
     * 🛡️ Constructor de compatibilidad para el WarManager.
     * Inicializa sets concurrentes para evitar colisiones entre Virtual Threads.
     */
    public WarContract(UUID warId, UUID clanAtacante, UUID clanDefensor, BigDecimal apuestaMonedas, long startTime, WarStatus status, int killsAtacante, int killsDefensor) {
        this(warId, clanAtacante, clanDefensor, apuestaMonedas, startTime, status, killsAtacante, killsDefensor, 10, 5,
                ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet(), 
                ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
    }

    public enum WarStatus {
        GRACE_PERIOD, // Tiempo de preparación (5 min)
        ACTIVE,       // En combate a muerte
        FINISHED      // Guerra terminada
    }

    /**
     * 🛡️ Validación de Participación Cross-Play (Concurrencia Atómica):
     * Comprueba el cupo según la plataforma y registra al combatiente sin riesgos de colisión.
     */
    public boolean registrarParticipante(UUID playerId, boolean isAtacante, boolean isBedrock) {
        Set<UUID> roster = isAtacante 
            ? (isBedrock ? atacanteBedrock : atacanteJava) 
            : (isBedrock ? defensorBedrock : defensorJava);

        // Lectura rápida (Sin bloqueo)
        if (roster.contains(playerId)) return true;

        int max = isBedrock ? maxBedrockSlots : maxJavaSlots;

        // 🛡️ Bloqueo Atómico (Mutex): Evita que 2 Hilos Virtuales llenen el último cupo simultáneamente
        synchronized (roster) {
            if (roster.size() < max) {
                return roster.add(playerId);
            }
        }

        // Cupo agotado para la plataforma específica
        return false;
    }
}
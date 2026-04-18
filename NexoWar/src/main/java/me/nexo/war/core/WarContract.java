package me.nexo.war.core;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record WarContract(
        UUID warId,
        UUID clanAtacante,
        UUID clanDefensor,
        BigDecimal apuestaMonedas,
        long startTime,
        WarStatus status,
        int killsAtacante,
        int killsDefensor,

        // 🌟 NUEVO: Módulo 1 (Cross-Play Slots)
        int maxJavaSlots,
        int maxBedrockSlots,
        Set<UUID> atacanteJava,
        Set<UUID> atacanteBedrock,
        Set<UUID> defensorJava,
        Set<UUID> defensorBedrock
) {
    // 🛡️ Constructor de compatibilidad (Para no romper el WarManager actual)
    public WarContract(UUID warId, UUID clanAtacante, UUID clanDefensor, BigDecimal apuestaMonedas, long startTime, WarStatus status, int killsAtacante, int killsDefensor) {
        // Configuramos 10 Cupos para PC y 5 para Móvil/Consola por defecto
        this(warId, clanAtacante, clanDefensor, apuestaMonedas, startTime, status, killsAtacante, killsDefensor, 10, 5,
                ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
    }

    public enum WarStatus {
        GRACE_PERIOD, // Tiempo de preparación (5 min)
        ACTIVE,       // En combate a muerte
        FINISHED      // Guerra terminada
    }

    /**
     * 🛡️ Método de Validación Cross-Play:
     * Comprueba si hay cupo para el jugador según su plataforma. Si hay espacio, lo inscribe en la guerra.
     */
    public boolean registrarParticipante(UUID playerId, boolean isAtacante, boolean isBedrock) {
        Set<UUID> roster = isAtacante ? (isBedrock ? atacanteBedrock : atacanteJava) : (isBedrock ? defensorBedrock : defensorJava);

        // Si ya está registrado en este conflicto, tiene luz verde
        if (roster.contains(playerId)) return true;

        // Si hay espacio en su plataforma, lo registramos como combatiente activo
        int max = isBedrock ? maxBedrockSlots : maxJavaSlots;
        if (roster.size() < max) {
            roster.add(playerId);
            return true;
        }

        // Cupo lleno para su plataforma
        return false;
    }
}
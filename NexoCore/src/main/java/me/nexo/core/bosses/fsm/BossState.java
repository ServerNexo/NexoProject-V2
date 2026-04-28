package me.nexo.core.bosses.fsm;

/**
 * ⚙️ Estados de la Máquina Finita (FSM) para la IA de Jefes.
 */
public enum BossState {
    SPAWN,          // Animación de entrada (Inmune)
    IDLE,           // Esperando jugadores
    CHASE,          // Persiguiendo al objetivo con más aggro
    MELEE_ATTACK,   // Golpeando cuerpo a cuerpo
    CASTING_SKILL,  // Lanzando una habilidad especial (Partículas asíncronas)
    DEATH           // Animación de muerte y drop de esencias
}
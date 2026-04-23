package me.nexo.items.artefactos;

import org.bukkit.entity.Player;

/**
 * 🎒 NexoItems - Patrón Estrategia para Artefactos (Arquitectura Enterprise Java 21)
 * Define el contrato estándar para todas las habilidades dinámicas del servidor.
 */
public interface ArtefactoStrategy {

    /**
     * Ejecuta la habilidad específica de un artefacto.
     * * @param jugador El jugador que activa el artefacto.
     * @param dto Los datos base del artefacto (costo de energía, cooldown, daño, etc.).
     * @return true si la habilidad se ejecutó con éxito (para cobrar la energía y aplicar CD), false si falló o fue cancelada.
     */
    boolean ejecutar(Player jugador, ArtefactoDTO dto);

}
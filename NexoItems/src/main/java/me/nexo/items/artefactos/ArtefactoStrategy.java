package me.nexo.items.artefactos;

import org.bukkit.entity.Player;

public interface ArtefactoStrategy {

    /**
     * Ejecuta la habilidad del artefacto.
     * * @param jugador El jugador que usa el artefacto.
     * @param dto Los datos base del artefacto (costo, cooldown, etc.).
     * @return true si la habilidad se ejecutó con éxito (para cobrar la energía y aplicar CD), false si falló o se canceló.
     */
    boolean ejecutar(Player jugador, ArtefactoDTO dto);

}
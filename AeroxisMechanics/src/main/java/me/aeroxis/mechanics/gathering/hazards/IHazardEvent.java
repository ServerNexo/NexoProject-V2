package me.aeroxis.mechanics.gathering.hazards;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * ☠️ Contrato para todos los Peligros (Hazards) de NexoGathering.
 */
public interface IHazardEvent {
    void execute(Player player, Location loc);
}
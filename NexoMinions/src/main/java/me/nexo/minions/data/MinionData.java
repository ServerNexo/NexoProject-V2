package me.nexo.minions.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;

import java.util.UUID;

/**
 * 🤖 NexoMinions - Modelo DTO (Data Transfer Object)
 * Rendimiento: Estructura de datos plana (Lombok).
 * Nota: Al ser un modelo efímero de datos, no interviene Guice.
 */
@Data
@AllArgsConstructor
public class MinionData {
    private UUID minionId;
    private UUID ownerId;
    private MinionType type;
    private int tier;
    private Location location;
    private long nextActionTime;
}
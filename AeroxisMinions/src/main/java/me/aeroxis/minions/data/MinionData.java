package me.aeroxis.minions.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;

import java.util.UUID;

/**
 * 🤖 AeroxisMinions - Modelo DTO (Data Transfer Object)
 * Rendimiento: Estructura de datos plana (Lombok). Adaptado al Omni-Minion.
 * Nota: Al ser un modelo efímero de datos, no interviene Guice.
 */
@Data
@AllArgsConstructor
public class MinionData {
    private UUID minionId;
    private UUID ownerId;
    private String currentProductionId; // 🌟 FASE 3: El Omni-Minion usa String
    private int tier;
    private Location location;
    private long nextActionTime;
}
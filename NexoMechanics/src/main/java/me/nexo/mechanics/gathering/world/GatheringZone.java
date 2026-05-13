package me.nexo.mechanics.gathering.world;

import me.nexo.mechanics.gathering.data.Profession;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;

/**
 * 🗺️ Define una zona física donde se aplican las mecánicas de NexoGathering.
 */
public record GatheringZone(
        String id,
        Profession profession,
        BoundingBox area,
        String worldName,
        Material depletedMaterial,
        int regenTimeTicks
) {
    public boolean contains(org.bukkit.Location loc) {
        return loc.getWorld().getName().equals(worldName) && area.contains(loc.toVector());
    }
}
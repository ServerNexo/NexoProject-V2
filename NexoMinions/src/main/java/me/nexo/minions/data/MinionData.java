package me.nexo.minions.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;

import java.util.UUID;

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
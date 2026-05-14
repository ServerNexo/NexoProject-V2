package me.aeroxis.core.api;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public interface AeroxisFactoriesAPI {
    void routeItem(Location origin, UUID targetLinkId, ItemStack item);
}
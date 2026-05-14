package me.nexo.items.api.lategame;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎒 Caché en Memoria de los Stats de Late-Game de los jugadores.
 */
public class LateGameStatsCache {
    
    private static final Map<UUID, LateGameArmorStats> playerStats = new ConcurrentHashMap<>();

    public static LateGameArmorStats getStats(Player player) {
        return playerStats.getOrDefault(player.getUniqueId(), LateGameArmorStats.empty());
    }

    public static void setStats(Player player, LateGameArmorStats stats) {
        playerStats.put(player.getUniqueId(), stats);
    }

    public static void clearStats(Player player) {
        playerStats.remove(player.getUniqueId());
    }
}
package me.nexo.items.accesorios;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AccessoryStatsUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Map<AccessoryDTO.StatType, Double> stats;
    private final int nexoPower;
    private final boolean tieneCorazonNexo;

    public AccessoryStatsUpdateEvent(Player player, Map<AccessoryDTO.StatType, Double> stats, int nexoPower, boolean tieneCorazonNexo) {
        this.player = player;
        this.stats = stats;
        this.nexoPower = nexoPower;
        this.tieneCorazonNexo = tieneCorazonNexo;
    }

    public Player getPlayer() { return player; }
    public Map<AccessoryDTO.StatType, Double> getStats() { return stats; }
    public int getNexoPower() { return nexoPower; }
    public boolean hasCorazonNexo() { return tieneCorazonNexo; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
package me.nexo.items.accesorios;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 🎒 NexoItems - Evento Custom de Actualización de Accesorios (Arquitectura Enterprise Java 21)
 * Evento inmutable y Thread-Safe para recalcular los atributos del jugador.
 */
public class AccessoryStatsUpdateEvent extends Event {
    
    // 🛡️ REQUISITO DE PAPER API: Los eventos custom DEBEN tener este campo estático.
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Map<AccessoryDTO.StatType, Double> stats;
    private final int nexoPower;
    private final boolean tieneCorazonNexo;

    public AccessoryStatsUpdateEvent(Player player, Map<AccessoryDTO.StatType, Double> stats, int nexoPower, boolean tieneCorazonNexo) {
        this.player = player;
        // 🌟 GARANTÍA DE INMUTABILIDAD: Protegemos los datos contra modificaciones externas (Race Conditions)
        this.stats = (stats == null) ? Map.of() : Map.copyOf(stats);
        this.nexoPower = nexoPower;
        this.tieneCorazonNexo = tieneCorazonNexo;
    }

    public Player getPlayer() { 
        return player; 
    }
    
    public Map<AccessoryDTO.StatType, Double> getStats() { 
        return stats; 
    }
    
    public int getNexoPower() { 
        return nexoPower; 
    }
    
    public boolean hasCorazonNexo() { 
        return tieneCorazonNexo; 
    }

    @NotNull
    @Override
    public HandlerList getHandlers() { 
        return HANDLERS; 
    }
    
    public static HandlerList getHandlerList() { 
        return HANDLERS; 
    }
}
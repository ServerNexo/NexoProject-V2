package me.nexo.core;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 🌟 Evento disparado cuando un jugador desbloquea un cosmético (Cajas, Rangos, etc.)
 */
public class NexoCosmeticUnlockEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerUuid;
    private final String cosmeticId;

    public NexoCosmeticUnlockEvent(UUID playerUuid, String cosmeticId) {
        super(true); // Es asíncrono-safe
        this.playerUuid = playerUuid;
        this.cosmeticId = cosmeticId;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getCosmeticId() { return cosmeticId; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
package me.nexo.chat.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.chat.managers.NexoChatManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NexoChatExpansion extends PlaceholderExpansion {

    private final NexoChatManager chatManager;

    public NexoChatExpansion(NexoChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public @NotNull String getIdentifier() { 
        return "nexochat"; 
    }

    @Override
    public @NotNull String getAuthor() { 
        return "Nexo"; 
    }

    @Override
    public @NotNull String getVersion() { 
        return "1.0"; 
    }
    
    @Override
    public boolean persist() {
        return true; // Mantiene la expansión activa al recargar PAPI
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        
        // Si usan %nexochat_color%, devolvemos el HEX o tag que tenga equipado
        if (params.equalsIgnoreCase("color")) {
            return chatManager.getPlayerCosmetic(player.getUniqueId());
        }
        
        return null;
    }
}
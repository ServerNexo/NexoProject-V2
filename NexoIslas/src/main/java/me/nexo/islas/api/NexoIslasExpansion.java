package me.nexo.islas.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandLevelEngine;
import me.nexo.islas.managers.IslandManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NexoIslasExpansion extends PlaceholderExpansion {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandLevelEngine levelEngine;

    public NexoIslasExpansion(NexoIslas plugin, IslandManager islandManager, IslandLevelEngine levelEngine) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.levelEngine = levelEngine;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexoislas"; // El prefijo de tus variables será %nexoislas_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "Arquitecto";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        // Buscamos la isla del jugador
        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());

        // Si el jugador no tiene isla, mostramos valores en 0
        if (profile == null) {
            if (params.equalsIgnoreCase("level")) return "0";
            if (params.equalsIgnoreCase("value")) return "0";
            if (params.equalsIgnoreCase("xp")) return "0.0";
            if (params.equalsIgnoreCase("xp_req")) return "1000.0";
            if (params.equalsIgnoreCase("progress")) return "0%";
            return "N/A";
        }

        // %nexoislas_level% -> Devuelve el nivel actual
        if (params.equalsIgnoreCase("level")) {
            return String.valueOf(profile.getLevel());
        }

        // %nexoislas_value% -> Devuelve el valor total (Cristales del Nexo)
        if (params.equalsIgnoreCase("value")) {
            return String.valueOf(profile.getValue());
        }

        // %nexoislas_xp% -> Devuelve la XP que tiene actualmente
        if (params.equalsIgnoreCase("xp")) {
            return String.format("%.1f", profile.getTotalXp());
        }

        // %nexoislas_xp_req% -> Devuelve la XP necesaria para el SIGUIENTE nivel
        if (params.equalsIgnoreCase("xp_req")) {
            return String.format("%.1f", levelEngine.getRequiredXp(profile.getLevel()));
        }

        // %nexoislas_progress% -> Devuelve el porcentaje de completado (Ej: 45.5%)
        if (params.equalsIgnoreCase("progress")) {
            double currentXp = profile.getTotalXp();
            double reqXp = levelEngine.getRequiredXp(profile.getLevel());
            double percent = (currentXp / reqXp) * 100.0;
            return String.format("%.1f%%", Math.min(percent, 100.0));
        }

        return null;
    }
}
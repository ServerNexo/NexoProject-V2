package me.nexo.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.core.user.NexoAPI;
import me.nexo.core.user.NexoUser;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class NexoExpansion extends PlaceholderExpansion {

    public NexoExpansion(NexoCore plugin) {
        // El parámetro del plugin es requerido por la API de PlaceholderAPI, pero no lo usamos internamente.
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexo";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TuNombre";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();

        NexoUser user = NexoAPI.getInstance().getUserLocal(uuid);

        if (user == null) {
            if (params.equalsIgnoreCase("nivel") || params.endsWith("_nivel")) return "1";
            if (params.equalsIgnoreCase("xp")) return "0";
            if (params.equalsIgnoreCase("xprequerida")) return "100";
            return "";
        }

        if (params.equalsIgnoreCase("nivel")) {
            return String.valueOf(user.getNexoNivel());
        }
        if (params.equalsIgnoreCase("xp")) {
            return String.valueOf(user.getNexoXp());
        }
        if (params.equalsIgnoreCase("xprequerida")) {
            int nivelActual = user.getNexoNivel();
            return String.valueOf(nivelActual * 100);
        }

        if (params.equalsIgnoreCase("mineria_nivel")) {
            return String.valueOf(user.getMineriaNivel());
        }
        if (params.equalsIgnoreCase("combate_nivel")) {
            return String.valueOf(user.getCombateNivel());
        }
        if (params.equalsIgnoreCase("agricultura_nivel")) {
            return String.valueOf(user.getAgriculturaNivel());
        }

        return null;
    }
}
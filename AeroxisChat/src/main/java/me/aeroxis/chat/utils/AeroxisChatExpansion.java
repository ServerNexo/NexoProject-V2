package me.aeroxis.chat.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.aeroxis.chat.managers.AeroxisChatManager;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.user.AeroxisAPI; // 🌟 IMPORTAMOS LA API
import me.aeroxis.core.user.AeroxisUser; // 🌟 IMPORTAMOS EL USUARIO
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class AeroxisChatExpansion extends PlaceholderExpansion {

    private final AeroxisChatManager chatManager;
    private AeroxisAPI aeroxisApi; // Caché de la API para alto rendimiento

    public AeroxisChatExpansion(AeroxisChatManager chatManager) {
        this.chatManager = chatManager;
    }

    /**
     * 🌟 Obtenemos la API directamente del Inyector del Core para evitar métodos obsoletos.
     */
    private AeroxisAPI getNexoApi() {
        if (this.aeroxisApi == null) {
            this.aeroxisApi = JavaPlugin.getPlugin(AeroxisCore.class).getInjector().getInstance(AeroxisAPI.class);
        }
        return this.aeroxisApi;
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

        // 🌟 Leemos el color nativamente desde el Core
        if (params.equalsIgnoreCase("color")) {
            AeroxisUser user = getNexoApi().getUserLocal(player.getUniqueId());
            return (user != null && user.getChatColor() != null) ? user.getChatColor() : "<gray>";
        }

        return null;
    }
}
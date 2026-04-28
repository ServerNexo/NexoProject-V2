package me.nexo.chat.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.chat.managers.NexoChatManager;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoAPI; // 🌟 IMPORTAMOS LA API
import me.nexo.core.user.NexoUser; // 🌟 IMPORTAMOS EL USUARIO
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class NexoChatExpansion extends PlaceholderExpansion {

    private final NexoChatManager chatManager;
    private NexoAPI nexoApi; // Caché de la API para alto rendimiento

    public NexoChatExpansion(NexoChatManager chatManager) {
        this.chatManager = chatManager;
    }

    /**
     * 🌟 Obtenemos la API directamente del Inyector del Core para evitar métodos obsoletos.
     */
    private NexoAPI getNexoApi() {
        if (this.nexoApi == null) {
            this.nexoApi = JavaPlugin.getPlugin(NexoCore.class).getInjector().getInstance(NexoAPI.class);
        }
        return this.nexoApi;
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
            NexoUser user = getNexoApi().getUserLocal(player.getUniqueId());
            return (user != null && user.getChatColor() != null) ? user.getChatColor() : "<gray>";
        }

        return null;
    }
}
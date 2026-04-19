package me.nexo.colecciones.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.CollectionProfile;
import me.nexo.colecciones.data.CollectionItem;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * 📚 NexoColecciones - Expansión de PlaceholderAPI (Arquitectura Enterprise)
 */
@Singleton
public class ColeccionesExpansion extends PlaceholderExpansion {

    private final NexoColecciones plugin;
    private final CollectionManager manager;

    // 💉 PILAR 3: Inyección de Dependencias Directa
    @Inject
    public ColeccionesExpansion(NexoColecciones plugin, CollectionManager manager) {
        this.plugin = plugin;
        this.manager = manager; // 🌟 Manager inyectado, sin acoplamiento
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexocolecciones"; // El inicio de tu variable: %nexocolecciones_...%
    }

    @Override
    public @NotNull String getAuthor() {
        // 🌟 FIX: Texto estático y limpio. (getAuthors().toString() devuelve "[Nombre]")
        return "NexoNetwork";
    }

    @Override
    public @NotNull String getVersion() {
        // 🌟 FIX: Actualizado al nuevo estándar de Paper 1.21.4 (PluginMeta)
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Mantiene la expansión activa al recargar PAPI (/papi reload)
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Validación de seguridad (PAPI a veces lanza NPCs o jugadores offline)
        if (player == null || !player.isOnline()) return "0";

        CollectionProfile profile = manager.getProfile(player.getUniqueId());
        if (profile == null) return "0";

        // 🌟 VARIABLE 1: %nexocolecciones_progress_ITEM%
        if (params.startsWith("progress_")) {
            String itemId = params.replace("progress_", "").toLowerCase();
            return String.valueOf(profile.getProgress(itemId));
        }

        // 🌟 VARIABLE 2: %nexocolecciones_level_ITEM%
        if (params.startsWith("level_")) {
            String itemId = params.replace("level_", "").toLowerCase();

            // Buscamos el ítem en la memoria RAM ultra-rápida
            CollectionItem item = manager.getItemGlobal(itemId);
            if (item == null) return "0"; // Si no existe, es nivel 0

            int progreso = profile.getProgress(itemId);

            // Calculamos el nivel basado en el progreso actual y los Tiers
            return String.valueOf(manager.calcularNivel(item, progreso));
        }

        return null; // Si escriben mal la variable
    }
}
package me.nexo.colecciones.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * 📚 NexoColecciones - Expansión de PlaceholderAPI (Arquitectura Enterprise)
 * Rendimiento: Micro-optimizaciones de Strings O(1) para evitar Lag en Scoreboards y Tablists.
 */
@Singleton
public class ColeccionesExpansion extends PlaceholderExpansion {

    private final NexoColecciones plugin;
    private final CollectionManager manager;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ColeccionesExpansion(NexoColecciones plugin, CollectionManager manager) {
        this.plugin = plugin;
        this.manager = manager; 
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexocolecciones"; // El inicio de tu variable: %nexocolecciones_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "NexoNetwork";
    }

    @Override
    public @NotNull String getVersion() {
        // 🌟 PAPER 1.21 FIX: getPluginMeta() es la forma nativa moderna (getDescription es legacy Bukkit)
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

        var profile = manager.getProfile(player.getUniqueId());
        if (profile == null) return "0";

        // 🌟 VARIABLE 1: %nexocolecciones_progress_ITEM%
        if (params.startsWith("progress_")) {
            // 🌟 MICRO-OPTIMIZACIÓN: substring() es 10x más rápido que replace() para PAPI
            String itemId = params.substring(9).toLowerCase();
            return String.valueOf(profile.getProgress(itemId));
        }

        // 🌟 VARIABLE 2: %nexocolecciones_level_ITEM%
        if (params.startsWith("level_")) {
            String itemId = params.substring(6).toLowerCase();

            // Buscamos el ítem en la memoria RAM ultra-rápida
            var item = manager.getItemGlobal(itemId);
            if (item == null) return "0"; // Si no existe, es nivel 0

            int progreso = profile.getProgress(itemId);

            // Calculamos el nivel basado en el progreso actual y los Tiers
            return String.valueOf(manager.calcularNivel(item, progreso));
        }

        return null; // Si escriben mal la variable
    }
}
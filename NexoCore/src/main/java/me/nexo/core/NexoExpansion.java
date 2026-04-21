package me.nexo.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 🏛️ Nexo Network - PlaceholderAPI Expansion
 * Arquitectura Enterprise: Cero referencias estáticas, inyección pura a través de Guice.
 */
@Singleton
public class NexoExpansion extends PlaceholderExpansion {

    private final UserManager userManager;

    // 💉 PILAR 3: Inyección de Dependencias del Manager directamente
    @Inject
    public NexoExpansion(UserManager userManager) {
        this.userManager = userManager;
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

        // 🌟 FIX: Acceso local directo desde la memoria inyectada, sin llamadas estáticas
        NexoUser user = userManager.getUserOrNull(uuid);

        if (user == null) {
            if (params.equalsIgnoreCase("nivel") || params.endsWith("_nivel")) return "1";
            if (params.equalsIgnoreCase("xp")) return "0";
            if (params.equalsIgnoreCase("xprequerida")) return "100";
            return "";
        }

        // ========================================================================
        // 💡 LÓGICA DE NEGOCIO INTACTA
        // ========================================================================
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
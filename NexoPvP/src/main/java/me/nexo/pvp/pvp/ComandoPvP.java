package me.nexo.pvp.pvp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🏛️ NexoPvP - Comando PvP (Arquitectura NATIVA)
 * Fusión de Ejecución directa, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoPvP extends Command {

    private final PvPManager manager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoPvP(PvPManager manager) {
        super("pvp"); // 🌟 Nombre nativo base
        this.setAliases(List.of("togglepvp")); // Alias nativos
        this.setPermission("nexopvp.user"); // 🔒 Permiso asignado
        this.setPermissionMessage("§c❌ No tienes autorización táctica para este comando.");

        this.manager = manager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // Validación de permisos nativa
        if (!testPermission(sender)) return true;

        // 💻 PROTECCIÓN DE CONSOLA
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] El terminal no puede participar en el combate físico.");
            return true;
        }

        // 🚀 Ejecución en O(1) de tu manager
        manager.togglePvP(player);

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // No requiere argumentos
        return Collections.emptyList();
    }
}
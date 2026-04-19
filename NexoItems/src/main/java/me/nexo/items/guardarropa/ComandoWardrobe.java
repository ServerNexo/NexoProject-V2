package me.nexo.items.guardarropa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🎒 NexoItems - Comando Principal del Guardarropa (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoWardrobe extends Command {

    private final NexoItems plugin;
    private final GuardarropaListener listener;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoWardrobe(NexoItems plugin, GuardarropaListener listener) {
        super("wardrobe"); // 🌟 Nombre nativo base
        this.setAliases(List.of("guardarropa", "armario")); // Alias nativos

        this.plugin = plugin;
        this.listener = listener;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 PROTECCIÓN DE CONSOLA
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] El guardarropa solo está disponible para jugadores en el plano físico.");
            return true;
        }

        // 1. Abrir Menú (/wardrobe o /guardarropa)
        if (args.length == 0) {
            listener.abrirMenu(player);
            return true;
        }

        // 2. Ayuda (/wardrobe help)
        if (args[0].equalsIgnoreCase("help")) {
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            CrossplayUtils.sendMessage(player, "&#ff00ff👔 <bold>SISTEMA DE GUARDARROPA</bold>");
            CrossplayUtils.sendMessage(player, "&#00f5ff/wardrobe &#E6CCFF- Abre tu armario de armaduras.");
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            return true;
        }

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        // Autocompletado rápido para el comando de ayuda
        if (args.length == 1) {
            return List.of("help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
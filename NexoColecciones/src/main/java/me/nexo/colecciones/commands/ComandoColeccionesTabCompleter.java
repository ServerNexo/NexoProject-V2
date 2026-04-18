package me.nexo.colecciones.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 📚 NexoColecciones - Autocompletado Inteligente (Arquitectura Enterprise)
 */
public class ComandoColeccionesTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            // 🌟 FIX: Autocompletado dinámico basado en permisos y usando Streams directos
            Stream<String> subCommands = sender.hasPermission("nexocolecciones.admin")
                    ? Stream.of("reload", "top")
                    : Stream.of("top");

            return subCommands
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList(); // 🌟 FIX: .toList() nativo de Java 16+ (Cero dependencias extra)
        }

        // 🌟 FIX: Retornamos una lista vacía inmutable.
        // Esto evita crear objetos basura en la RAM si un jugador spammea la tecla Tabulador.
        return Collections.emptyList();
    }
}
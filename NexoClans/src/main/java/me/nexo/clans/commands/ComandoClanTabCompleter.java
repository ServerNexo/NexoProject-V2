package me.nexo.clans.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 👥 NexoClans - Autocompletado Inteligente (Arquitectura Enterprise)
 * Rendimiento: Cero instanciación de objetos basura, listas inmutables nativas (Java 21).
 */
public class ComandoClanTabCompleter implements TabCompleter {

    // 🌟 FIX: List.of() es nativo, inmutable y más rápido que Arrays.asList()
    private static final List<String> SUB_COMMANDS = List.of(
            "create", "invite", "join", "leave", "ff", "friendlyfire",
            "kick", "disband", "deposit", "withdraw", "sethome", "home", "tribute"
    );

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList(); // 🌟 FIX: .toList() directo de Java 16+
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        // 🌟 FIX: Retornamos una lista inmutable estática.
        // Esto evita que el Garbage Collector trabaje horas extras.
        return Collections.emptyList();
    }
}
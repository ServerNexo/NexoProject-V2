package me.nexo.economy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 💰 NexoEconomy - Autocompletado del Comando Eco (Arquitectura Enterprise)
 */
public class ComandoEcoTabCompleter implements TabCompleter {

    private static final List<String> CURRENCIES = List.of("COINS", "GEMS", "MANA");
    private static final List<String> AMOUNTS = List.of("100", "500", "1000", "5000", "10000");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        // 🛡️ SEGURIDAD VISUAL: Si no es administrador, no le sugerimos nada.
        // El comando /eco base (ver balance) no requiere argumentos.
        if (!sender.hasPermission("nexoeconomy.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return List.of("give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return CURRENCIES.stream()
                    .filter(s -> s.startsWith(args[2].toUpperCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return AMOUNTS.stream()
                    .filter(s -> s.startsWith(args[3]))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
package me.nexo.dungeons.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🏰 NexoDungeons - Autocompletado del Comando Principal (Arquitectura Enterprise)
 */
public class ComandoDungeonTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        // 🛡️ Aquí puedes agregar filtros en el futuro si añades subcomandos.
        // Ej: if (sender.hasPermission("nexodungeons.admin") && args.length == 1) { return List.of("reload", "forcestart"); }

        // 🌟 FIX: Retornamos una lista vacía inmutable.
        // Esto evita crear objetos basura en la RAM si un jugador spammea la tecla Tabulador.
        return Collections.emptyList();
    }
}
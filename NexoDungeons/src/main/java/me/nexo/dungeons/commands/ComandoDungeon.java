package me.nexo.dungeons.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.matchmaking.QueueManager;
import me.nexo.dungeons.menu.DungeonMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🏰 NexoDungeons - Comando Principal (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap Nativo Paper 1.21.5, TabCompleter Fusionado e Inyección Transitiva.
 */
@Singleton
public class ComandoDungeon extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS (Para enviar al DungeonMenu)
    private final QueueManager queueManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ComandoDungeon(QueueManager queueManager, CrossplayUtils crossplayUtils) {
        super("dungeons");

        // 🌟 FIX ERROR ALIASES: Usamos los Setters oficiales para mantener el encapsulamiento
        this.setDescription("Abre el menú holográfico de las mazmorras.");
        this.setAliases(List.of("dungeon", "mazmorras", "instancias"));

        this.queueManager = queueManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 FIX: Protección de consola segura (Java Pattern Matching)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] Acceso denegado: El terminal no puede abrir el menú holográfico de las mazmorras.");
            return true;
        }

        // 🌟 ABRE EL MENÚ AL INSTANTE: Inyectando las dependencias requeridas
        new DungeonMenu(player, queueManager, crossplayUtils).open();

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // 🌟 FIX: Retornamos una lista vacía inmutable directamente desde el comando.
        // Evita crear objetos basura en la RAM si un jugador spammea la tecla Tabulador.
        return Collections.emptyList();
    }
}
package me.nexo.dungeons.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.menu.DungeonMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🏰 NexoDungeons - Comando Principal (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoDungeon extends Command {

    private final NexoDungeons plugin;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoDungeon(NexoDungeons plugin) {
        super("dungeon"); // 🌟 Nombre nativo base
        this.setAliases(List.of("dungeons", "mazmorras", "mazmorra")); // Alias nativos
        this.plugin = plugin;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 FIX: Protección de consola segura y sin variables estáticas innecesarias.
        // La consola de Windows/Linux no procesa bien el Hexadecimal, se envía texto plano.
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] Acceso denegado: El terminal no puede abrir el menú holográfico de las mazmorras.");
            return true;
        }

        // 🌟 ABRE EL MENÚ AL INSTANTE
        new DungeonMenu(player, plugin).open();

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // 🛡️ Aquí puedes agregar filtros en el futuro si añades subcomandos.
        // Ej: if (sender.hasPermission("nexodungeons.admin") && args.length == 1) { return List.of("reload", "forcestart"); }

        // 🌟 FIX: Retornamos una lista vacía inmutable.
        // Esto evita crear objetos basura en la RAM si un jugador spammea la tecla Tabulador.
        return Collections.emptyList();
    }
}
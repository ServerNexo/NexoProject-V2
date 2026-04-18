package me.nexo.dungeons.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.menu.DungeonMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 🏰 NexoDungeons - Comando Principal (Arquitectura Enterprise)
 */
@Singleton
public class ComandoDungeon implements CommandExecutor {

    private final NexoDungeons plugin;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoDungeon(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

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
}
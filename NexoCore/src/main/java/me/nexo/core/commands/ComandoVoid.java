package me.nexo.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton; // 🌟 IMPORTACIÓN NECESARIA
import me.nexo.core.config.ConfigManager;
import me.nexo.core.user.UserManager;
import me.nexo.core.menus.VoidBlessingMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏛️ Nexo Network - Comando Void (Arquitectura NATIVA)
 * Actúa como una Fábrica: Inyecta dependencias y las pasa al menú.
 */
@Singleton // 🌟 FIX ENTERPRISE: Previene instanciación múltiple por Guice
public class ComandoVoid extends Command {

    // 💉 PILAR 3: Inyectamos solo las herramientas exactas
    private final UserManager userManager;
    private final ConfigManager configManager;

    @Inject
    public ComandoVoid(UserManager userManager, ConfigManager configManager) {
        super("void"); // Nombre nativo
        this.setPermission("nexocore.commands.void");
        this.setPermissionMessage("§cNo tienes permiso para usar este comando.");

        this.userManager = userManager;
        this.configManager = configManager;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return true;
        }

        // Le pasamos las herramientas al menú, NO el plugin entero.
        new VoidBlessingMenu(userManager, configManager, player).openMenu();
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return new ArrayList<>(); // No tiene autocompletados
    }
}
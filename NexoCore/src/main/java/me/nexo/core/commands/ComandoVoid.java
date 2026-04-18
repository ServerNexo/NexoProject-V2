package me.nexo.core.commands;

import com.google.inject.Inject;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.user.UserManager;
import me.nexo.core.menus.VoidBlessingMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ Nexo Network - Comando Void (Arquitectura Enterprise)
 * Actúa como una Fábrica: Inyecta dependencias y las pasa al menú.
 */
public class ComandoVoid {

    // 💉 PILAR 3: Inyectamos solo las herramientas exactas
    private final UserManager userManager;
    private final ConfigManager configManager;

    @Inject
    public ComandoVoid(UserManager userManager, ConfigManager configManager) {
        this.userManager = userManager;
        this.configManager = configManager;
    }

    @Command("void")
    @CommandPermission("nexocore.commands.void")
    public void invocarVacio(Player player) {
        // Le pasamos las herramientas al menú, NO el plugin entero.
        new VoidBlessingMenu(userManager, configManager, player).openMenu();
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
    }
}
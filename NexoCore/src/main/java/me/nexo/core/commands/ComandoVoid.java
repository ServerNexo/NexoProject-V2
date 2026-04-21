package me.nexo.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.user.UserManager;
import me.nexo.core.menus.VoidBlessingMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ Nexo Network - Comando Void (Arquitectura Enterprise)
 * Actúa como una Fábrica: Inyecta dependencias y las pasa al menú temporal.
 */
@Singleton // 🌟 FIX CRÍTICO: Garantiza una única instancia para el CommandHandler de Lamp
public class ComandoVoid {

    // 💉 PILAR 1: Inyectamos solo las herramientas exactas
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
        // Le pasamos las herramientas al menú temporal, NO el plugin entero.
        new VoidBlessingMenu(userManager, configManager, player).openMenu();
        
        // Efecto de sonido inmersivo nativo de la API 1.21.5
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
    }
}
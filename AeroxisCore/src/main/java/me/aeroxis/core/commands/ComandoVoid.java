package me.aeroxis.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.menus.VoidBlessingMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ Aeroxis Network - Comando Void (Arquitectura Enterprise)
 * Actúa como una Fábrica: Inyecta dependencias y las pasa al menú temporal.
 */
@Singleton // 🌟 FIX CRÍTICO: Garantiza una única instancia para el CommandHandler de Lamp
public class ComandoVoid {

    // 💉 PILAR 1: Inyectamos solo las herramientas exactas
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    @Inject
    public ComandoVoid(UserManager userManager, CrossplayUtils crossplayUtils) {
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Command("void")
    @CommandPermission("aeroxiscore.commands.void")
    public void invocarVacio(Player player) {
        // 🌟 FIX: Orden correcto y uso de CrossplayUtils en lugar de ConfigManager
        // 🌟 FIX: Llamada correcta a .open() heredada de AeroxisMenu
        new VoidBlessingMenu(player, userManager, crossplayUtils).open();

        // Efecto de sonido inmersivo nativo de la API 1.21.5
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
    }
}
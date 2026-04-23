package me.nexo.pvp.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import me.nexo.economy.managers.EconomyManager; // Sinergia inyectada
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.menus.BlessingMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ NexoPvP - Comando Templo (Arquitectura Enterprise)
 * Actúa como una Fábrica: Inyecta herramientas y las pasa al menú.
 */
@Singleton // 🌟 FIX CRÍTICO: Garantiza instancia única en memoria
public class ComandoTemplo {

    private final ConfigManager configManager;
    private final UserRepository userRepository;
    private final UserManager userManager;
    private final NexoPvP plugin;
    
    // 🌟 Sinergia para el menú de bendiciones
    private final CrossplayUtils crossplayUtils;
    private final EconomyManager economyManager;

    // 💉 PILAR 1: Inyección Pura de todas las dependencias
    @Inject
    public ComandoTemplo(ConfigManager configManager, UserRepository userRepository, UserManager userManager, 
                         NexoPvP plugin, CrossplayUtils crossplayUtils, EconomyManager economyManager) {
        this.configManager = configManager;
        this.userRepository = userRepository;
        this.userManager = userManager;
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
        this.economyManager = economyManager;
    }

    @Command("templo")
    @CommandPermission("nexopvp.templo")
    public void abrirTemplo(Player player) {
        // 🚀 Le pasamos las herramientas limpias al menú (Fábrica)
        new BlessingMenu(player, configManager, userManager, userRepository, plugin, crossplayUtils, economyManager).open();
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2.0f);
    }
}
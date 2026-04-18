package me.nexo.pvp.commands;

import com.google.inject.Inject;
import me.nexo.core.NexoCore;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
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
public class ComandoTemplo {

    private final ConfigManager configManager;
    private final UserRepository userRepository;
    private final UserManager userManager;
    private final NexoPvP plugin;

    @Inject
    public ComandoTemplo(ConfigManager configManager, UserRepository userRepository, NexoPvP plugin) {
        this.configManager = configManager;
        this.userRepository = userRepository;
        this.plugin = plugin;
        // Obtenemos el gestor de usuarios del Core directamente para pasarlo al menú
        this.userManager = NexoCore.getPlugin(NexoCore.class).getUserManager();
    }

    @Command("templo")
    @CommandPermission("nexopvp.templo")
    public void abrirTemplo(Player player) {
        // 🚀 Le pasamos las herramientas limpias al menú
        new BlessingMenu(player, configManager, userManager, userRepository, plugin).open();
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2.0f);
    }
}
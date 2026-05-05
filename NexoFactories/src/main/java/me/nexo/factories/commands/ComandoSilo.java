package me.nexo.factories.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.listeners.LogisticsLinkerListener;
import me.nexo.factories.managers.SiloManager;
import me.nexo.factories.menu.SiloMenu;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor; // 🌟 CAMBIO AQUÍ
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * ☁️ NexoFactories - Terminal de la Nube (Silo VIP)
 * Permite gestionar recursos a distancia mediante base de datos.
 */
@Singleton
@Command({"silo", "terminal"})
public class ComandoSilo {

    private final NexoFactories plugin;
    private final LogisticsLinkerListener linkerListener;
    private final CrossplayUtils crossplayUtils;
    private final SiloManager siloManager;

    @Inject
    public ComandoSilo(NexoFactories plugin, LogisticsLinkerListener linkerListener, CrossplayUtils crossplayUtils, SiloManager siloManager) {
        this.plugin = plugin;
        this.linkerListener = linkerListener;
        this.crossplayUtils = crossplayUtils;
        this.siloManager = siloManager;
    }

    @Subcommand("link")
    @CommandPermission("nexofactories.vip.silo")
    public void linkCloud(Player player) {
        linkerListener.linkToCloud(player);
    }

    // 🌟 USAMOS @DefaultFor PARA EL COMANDO BASE
    @DefaultFor({"silo", "terminal"})
    @CommandPermission("nexofactories.vip.silo")
    public void openTerminal(Player player) {
        new SiloMenu(player, plugin, crossplayUtils, siloManager).open();
    }
}
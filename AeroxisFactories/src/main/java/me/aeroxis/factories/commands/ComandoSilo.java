package me.aeroxis.factories.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.factories.AeroxisFactories;
import me.aeroxis.factories.listeners.LogisticsLinkerListener;
import me.aeroxis.factories.managers.SiloManager;
import me.aeroxis.factories.menu.SiloMenu;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor; // 🌟 CAMBIO AQUÍ
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * ☁️ AeroxisFactories - Terminal de la Nube (Silo VIP)
 * Permite gestionar recursos a distancia mediante base de datos.
 */
@Singleton
@Command({"silo", "terminal"})
public class ComandoSilo {

    private final AeroxisFactories plugin;
    private final LogisticsLinkerListener linkerListener;
    private final CrossplayUtils crossplayUtils;
    private final SiloManager siloManager;

    @Inject
    public ComandoSilo(AeroxisFactories plugin, LogisticsLinkerListener linkerListener, CrossplayUtils crossplayUtils, SiloManager siloManager) {
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
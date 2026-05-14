package me.aeroxis.tools;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.tools.commands.LocationCommands;
import me.aeroxis.tools.commands.TpaCommands;
import me.aeroxis.tools.commands.UtilityCommands;
import revxrsal.commands.bukkit.BukkitCommandHandler;

@Singleton
public class ToolsBootstrap {

    private final AeroxisTools plugin;

    @Inject
    public ToolsBootstrap(AeroxisTools plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // Configuramos Lamp para el registro de comandos (Versión 3.2)
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // Registramos nuestras clases de comandos inyectadas con Guice
        handler.register(
                plugin.getInjector().getInstance(TpaCommands.class),
                plugin.getInjector().getInstance(LocationCommands.class),
                plugin.getInjector().getInstance(UtilityCommands.class)
        );
    }
}
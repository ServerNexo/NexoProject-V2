package me.nexo.war;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.war.commands.ComandoWar;
import me.nexo.war.listeners.WarCrossplayListener;
import me.nexo.war.listeners.WarListener;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoWar - Orquestador Enterprise
 */
@Singleton
public class WarBootstrap {

    private final NexoWar plugin;
    private final Server server;
    private final Injector injector;

    @Inject
    public WarBootstrap(NexoWar plugin, Injector injector) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoWar Enterprise");

        registerEvents();
        registerCommands();

        plugin.getLogger().info("⚔️ NexoWar activado e inyectado con éxito.");
    }

    public void stopServices() {
        plugin.getLogger().info("⚔️ NexoWar apagado.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();
        // 🚀 Registramos los listeners inyectados por Guice
        pm.registerEvents(injector.getInstance(WarListener.class), plugin);
        pm.registerEvents(injector.getInstance(WarCrossplayListener.class), plugin);
    }

    private void registerCommands() {
        // 💡 Inicializamos el motor de Lamp
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // 🛡️ CONTROL GLOBAL DE PERMISOS
        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error("§c❌ No tienes autorización táctica para este comando.");
        });

        // 💉 Inyectamos y registramos el comando mágico
        handler.register(injector.getInstance(ComandoWar.class));
    }
}
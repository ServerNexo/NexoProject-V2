package me.nexo.war;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.war.commands.ComandoWar;
import me.nexo.war.listeners.WarCrossplayListener;
import me.nexo.war.listeners.WarListener;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoWar - Orquestador Enterprise
 * Conectado orgánicamente al Inyector de NexoCore.
 */
@Singleton
public class WarBootstrap {

    private final NexoWar plugin;
    private final Server server;
    private final Injector injector;
    
    // 🌟 Sinergia de Módulos: Obtenemos utilidades directamente del Core
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public WarBootstrap(NexoWar plugin, Injector injector, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
        this.crossplayUtils = crossplayUtils;
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
            // 🌟 MODERNIZACIÓN: Adaptación estricta para Crossplay y colores Hex/MiniMessage
            BukkitCommandActor bukkitActor = (BukkitCommandActor) actor;
            
            if (bukkitActor.isPlayer()) {
                crossplayUtils.sendMessage(bukkitActor.getAsPlayer(), "&#8b0000[!] No tienes autorización táctica para este comando.");
            } else {
                // Si es la consola
                bukkitActor.getSender().sendMessage(crossplayUtils.parseCrossplay(null, "&#8b0000[!] No tienes autorización táctica para este comando."));
            }
        });

        // 💉 Inyectamos y registramos el comando principal
        handler.register(injector.getInstance(ComandoWar.class));
    }
}
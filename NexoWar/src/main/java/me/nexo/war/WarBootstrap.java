package me.nexo.war;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.war.commands.ComandoWar;
import me.nexo.war.listeners.WarCrossplayListener;
import me.nexo.war.listeners.WarListener;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

/**
 * 🏛️ NexoWar - Orquestador Enterprise (NATIVO)
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
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoWar Enterprise (NATIVA)");

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
        try {
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(server);

            // 💉 Inyectamos el comando saltándonos la seguridad estricta de Paper
            commandMap.register("nexowar", injector.getInstance(ComandoWar.class));

            plugin.getLogger().info("✅ Comandos de NexoWar inyectados nativamente (Zero-Lag).");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error inyectando comandos de NexoWar: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
package me.nexo.pvp.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.classes.ArmorClassListener;
import me.nexo.pvp.commands.ComandoTemplo;
import me.nexo.pvp.mechanics.DeathPenaltyListener;
import me.nexo.pvp.mechanics.TrainingStationListener;
import me.nexo.pvp.pasivas.PasivasListener;
import me.nexo.pvp.pvp.ComandoPvP;
import me.nexo.pvp.pvp.PvPListener;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

@Singleton
public class PvPBootstrap {

    private final NexoPvP plugin;
    private final Server server;
    private final Injector injector;

    // 💉 PILAR 3: Inyección Limpia (Solo pedimos lo que usamos)
    @Inject
    public PvPBootstrap(NexoPvP plugin, Injector injector) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoPvP Enterprise");

        registerEvents();
        registerCommands();

        plugin.getLogger().info("⚔️ NexoPvP activado e inyectado con éxito.");
    }

    public void stopServices() {
        plugin.getLogger().info("⚔️ NexoPvP apagado.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        // 🚀 Eventos Purificados y Desacoplados
        pm.registerEvents(injector.getInstance(PvPListener.class), plugin);
        pm.registerEvents(injector.getInstance(PasivasListener.class), plugin);
        pm.registerEvents(injector.getInstance(ArmorClassListener.class), plugin);
        pm.registerEvents(injector.getInstance(DeathPenaltyListener.class), plugin);
        pm.registerEvents(injector.getInstance(TrainingStationListener.class), plugin);
    }

    private void registerCommands() {
        // 💡 Inicializamos el motor de Lamp para NexoPvP
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // 🛡️ CONTROL GLOBAL DE PERMISOS (Mensaje personalizado)
        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error("§c❌ No tienes autorización táctica para este comando.");
        });

        // 💉 Usamos a Guice para construir e inyectar los comandos
        handler.register(injector.getInstance(ComandoPvP.class));
        handler.register(injector.getInstance(ComandoTemplo.class));
    }
}
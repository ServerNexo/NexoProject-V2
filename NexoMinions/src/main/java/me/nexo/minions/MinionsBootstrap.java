package me.nexo.minions;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.core.user.NexoAPI;
import me.nexo.minions.commands.ComandoMinion;
import me.nexo.minions.listeners.ExplosionListener;
import me.nexo.minions.listeners.MinionInteractListener;
import me.nexo.minions.listeners.MinionLoadListener;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoMinions - Orquestador Enterprise
 */
@Singleton
public class MinionsBootstrap {

    private final NexoMinions plugin;
    private final Server server;
    private final Injector injector;
    private final MinionManager minionManager;

    @Inject
    public MinionsBootstrap(NexoMinions plugin, Injector injector, MinionManager minionManager) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
        this.minionManager = minionManager;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoMinions Enterprise");

        // 🌟 Registrar API de Minions
        NexoAPI.getServices().register(MinionManager.class, minionManager);

        registerEvents();
        registerCommands();

        // 🌟 Arrancar el reloj interno de los Minions
        server.getScheduler().runTaskTimer(plugin, () -> minionManager.tickAll(System.currentTimeMillis()), 20L, 20L);

        plugin.getLogger().info("🤖 NexoMinions activado e inyectado con éxito.");
    }

    public void stopServices() {
        if (minionManager != null) {
            minionManager.saveAllMinionsSync();
        }
        NexoAPI.getServices().unregister(MinionManager.class);
        plugin.getLogger().info("🤖 NexoMinions apagado.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();
        // NOTA: Estas clases darán error temporalmente hasta que les inyectemos Guice en el próximo paso
        pm.registerEvents(injector.getInstance(MinionInteractListener.class), plugin);
        pm.registerEvents(injector.getInstance(MinionLoadListener.class), plugin);
        pm.registerEvents(injector.getInstance(ExplosionListener.class), plugin);
    }

    private void registerCommands() {
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error(plugin.getConfigManager().getMessages().comandos().sinPermiso());
        });

        handler.register(injector.getInstance(ComandoMinion.class));
    }
}
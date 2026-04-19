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
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

/**
 * 🏛️ NexoMinions - Orquestador Enterprise (NATIVO)
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
        registerCommands(); // 🌟 FIX: Registro Nativo (Sin frameworks)

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
        pm.registerEvents(injector.getInstance(MinionInteractListener.class), plugin);
        pm.registerEvents(injector.getInstance(MinionLoadListener.class), plugin);
        pm.registerEvents(injector.getInstance(ExplosionListener.class), plugin);
    }

    private void registerCommands() {
        try {
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(server);

            // 💉 Inyectamos el comando saltándonos la seguridad estricta de Paper
            commandMap.register("nexominions", injector.getInstance(ComandoMinion.class));

            plugin.getLogger().info("✅ Comandos de NexoMinions inyectados nativamente (Zero-Lag).");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error inyectando comandos de NexoMinions: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
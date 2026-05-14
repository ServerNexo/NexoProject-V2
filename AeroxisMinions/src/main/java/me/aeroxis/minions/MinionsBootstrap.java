package me.aeroxis.minions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.minions.commands.ComandoMinion;
import me.aeroxis.minions.config.ConfigManager;
import me.aeroxis.minions.listeners.ExplosionListener;
import me.aeroxis.minions.listeners.MinionInteractListener;
import me.aeroxis.minions.listeners.MinionListener; // 🌟 FIX CRÍTICO: Importamos el listener
import me.aeroxis.minions.listeners.MinionLoadListener;
import me.aeroxis.minions.manager.MinionManager;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ AeroxisMinions - Orquestador Enterprise
 * Rendimiento: Inyección Explícita (Fail-Fast), Cero Service Locators y Registro Nativo.
 */
@Singleton
public class MinionsBootstrap {

    private final AeroxisMinions plugin;
    private final Server server;
    private final MinionManager minionManager;
    private final ConfigManager configManager;

    private final MinionInteractListener interactListener;
    private final MinionLoadListener loadListener;
    private final ExplosionListener explosionListener;
    private final MinionListener minionListener; // 🌟 FIX CRÍTICO: Declaramos la variable
    private final ComandoMinion comandoMinion;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public MinionsBootstrap(AeroxisMinions plugin, MinionManager minionManager, ConfigManager configManager,
                            MinionInteractListener interactListener, MinionLoadListener loadListener,
                            ExplosionListener explosionListener, MinionListener minionListener, // 🌟 FIX CRÍTICO: Lo inyectamos
                            ComandoMinion comandoMinion) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.minionManager = minionManager;
        this.configManager = configManager;

        this.interactListener = interactListener;
        this.loadListener = loadListener;
        this.explosionListener = explosionListener;
        this.minionListener = minionListener; // 🌟 FIX CRÍTICO: Lo guardamos
        this.comandoMinion = comandoMinion;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura AeroxisMinions Enterprise");

        registerEvents();
        registerCommands();

        // 🌟 Arrancar el reloj interno de los Minions (Mantenido en el hilo principal por seguridad API Bukkit)
        server.getScheduler().runTaskTimer(plugin, () -> minionManager.tickAll(System.currentTimeMillis()), 20L, 20L);

        plugin.getLogger().info("🤖 AeroxisMinions activado e inyectado con éxito.");
    }

    public void stopServices() {
        if (minionManager != null) {
            // Síncrono y seguro para el apagado de la máquina (Regla 3)
            minionManager.saveAllMinionsSync();
        }
        plugin.getLogger().info("🤖 AeroxisMinions apagado.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();
        pm.registerEvents(interactListener, plugin);
        pm.registerEvents(loadListener, plugin);
        pm.registerEvents(explosionListener, plugin);
        pm.registerEvents(minionListener, plugin); // 🌟 FIX CRÍTICO: ¡Registramos el evento para que se puedan colocar!
    }

    private void registerCommands() {
        // Revxrsal BukkitCommandHandler ya maneja su inyección nativa en el CommandMap
        var handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error(configManager.getMessages().comandos().sinPermiso());
        });

        handler.register(comandoMinion);
    }
}
package me.nexo.minions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.minions.commands.ComandoMinion;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.listeners.ExplosionListener;
import me.nexo.minions.listeners.MinionInteractListener;
import me.nexo.minions.listeners.MinionLoadListener;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoMinions - Orquestador Enterprise
 * Rendimiento: Inyección Explícita (Fail-Fast), Cero Service Locators y Registro Nativo.
 */
@Singleton
public class MinionsBootstrap {

    private final NexoMinions plugin;
    private final Server server;
    private final MinionManager minionManager;
    private final ConfigManager configManager;

    // 🌟 INYECCIÓN EXPLÍCITA: Declaramos todas las dependencias para garantizar
    // que Guice valide su existencia en el mismo instante de arrancar (Fail-Fast).
    private final MinionInteractListener interactListener;
    private final MinionLoadListener loadListener;
    private final ExplosionListener explosionListener;
    private final ComandoMinion comandoMinion;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public MinionsBootstrap(NexoMinions plugin, MinionManager minionManager, ConfigManager configManager,
                            MinionInteractListener interactListener, MinionLoadListener loadListener,
                            ExplosionListener explosionListener, ComandoMinion comandoMinion) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.minionManager = minionManager;
        this.configManager = configManager;
        
        this.interactListener = interactListener;
        this.loadListener = loadListener;
        this.explosionListener = explosionListener;
        this.comandoMinion = comandoMinion;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoMinions Enterprise");

        registerEvents();
        registerCommands();

        // 🌟 Arrancar el reloj interno de los Minions (Mantenido en el hilo principal por seguridad API Bukkit)
        server.getScheduler().runTaskTimer(plugin, () -> minionManager.tickAll(System.currentTimeMillis()), 20L, 20L);

        plugin.getLogger().info("🤖 NexoMinions activado e inyectado con éxito.");
    }

    public void stopServices() {
        if (minionManager != null) {
            // Síncrono y seguro para el apagado de la máquina (Regla 3)
            minionManager.saveAllMinionsSync();
        }
        plugin.getLogger().info("🤖 NexoMinions apagado.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();
        // 🌟 FIX: Registro directo de las instancias inyectadas
        pm.registerEvents(interactListener, plugin);
        pm.registerEvents(loadListener, plugin);
        pm.registerEvents(explosionListener, plugin);
    }

    private void registerCommands() {
        // Revxrsal BukkitCommandHandler ya maneja su inyección nativa en el CommandMap
        var handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            // 🌟 FIX: Consumo del ConfigManager inyectado en lugar de usar getters estáticos o del Plugin
            actor.error(configManager.getMessages().comandos().sinPermiso());
        });

        handler.register(comandoMinion);
    }
}
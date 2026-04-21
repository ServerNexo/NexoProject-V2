package me.nexo.core.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import me.nexo.core.NexoCore;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import me.nexo.core.user.NexoUser;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.PlayerListener;
import me.nexo.core.HudTask;
import me.nexo.core.NexoExpansion;
import me.nexo.core.commands.ComandoNexo;
import me.nexo.core.commands.ComandoVoid;
import me.nexo.core.commands.WebCommand;
import me.nexo.core.crossplay.BedrockBugFixListener;
import me.nexo.core.hub.NexoMenuListener;
import me.nexo.core.listeners.VoidEssenceListener;
import me.nexo.core.menus.MenuGlobalListener;
import me.nexo.core.menus.VoidBlessingMenuListener;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.logging.Logger;

/**
 * 🏛️ Nexo Network - Service Bootstrap (Arquitectura Enterprise)
 * Orquestador central del ciclo de vida del plugin. Todo se enlaza a través de Guice.
 */
@Singleton
public class ServiceBootstrap {

    private final NexoCore plugin;
    private final Server server;
    private final Logger logger;
    private final Injector injector;

    // Dependencias inyectadas automáticamente
    private final DatabaseManager databaseManager;
    private final UserManager userManager;
    private final UserRepository userRepository;
    private final NexoWebServer webServer;
    private final ConfigManager configManager;

    // 💉 PILAR 1: Inyección maestra
    @Inject
    public ServiceBootstrap(NexoCore plugin, Server server, Injector injector,
                            DatabaseManager databaseManager, UserManager userManager,
                            UserRepository userRepository, NexoWebServer webServer,
                            ConfigManager configManager) {
        this.plugin = plugin;
        this.server = server;
        this.logger = plugin.getLogger();
        this.injector = injector;
        this.databaseManager = databaseManager;
        this.userManager = userManager;
        this.userRepository = userRepository;
        this.webServer = webServer;
        this.configManager = configManager;
    }

    public void startServices() {
        logger.info("========================================");
        logger.info("⚡ Arrancando Arquitectura Nexo Enterprise");
        logger.info("========================================");

        // 1. Inicializar Bases de Datos
        databaseManager.conectar();

        // 2. Iniciar API Web
        webServer.start();

        // 3. Registrar Eventos
        registerEvents();

        // 4. Tareas en Segundo Plano (🌟 FIX: Extraído de Guice)
        injector.getInstance(HudTask.class).runTaskTimer(plugin, 20L, 20L);

        // 5. Hooks Externos (🌟 FIX: Extraído de Guice)
        if (server.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            injector.getInstance(NexoExpansion.class).register();
        }

        // 6. Registro de comandos moderno (Lamp)
        registerCommands();

        logger.info("¡Nexo Core V8.2: Core Purificado al 100% y API Web en línea!");
    }

    public void stopServices() {
        if (webServer != null) {
            webServer.stop();
        }

        // 🗄️ PILAR 3: Guardado Seguro Síncrono (Main Thread) para evitar Race Conditions en el apagado.
        for (Player p : server.getOnlinePlayers()) {
            NexoUser user = userManager.getUserOrNull(p.getUniqueId());
            if (user != null) {
                userRepository.saveUserSync(user);
            }
        }

        if (databaseManager != null) {
            databaseManager.desconectar();
        }

        logger.info("NexoCore apagado y datos guardados de forma segura.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        // 💉 Todos los listeners son ahora Singletons administrados por Guice
        pm.registerEvents(injector.getInstance(PlayerListener.class), plugin);
        pm.registerEvents(injector.getInstance(VoidBlessingMenuListener.class), plugin);
        pm.registerEvents(injector.getInstance(VoidEssenceListener.class), plugin);
        pm.registerEvents(injector.getInstance(NexoMenuListener.class), plugin);
        pm.registerEvents(injector.getInstance(MenuGlobalListener.class), plugin);
        pm.registerEvents(injector.getInstance(BedrockBugFixListener.class), plugin);
    }

    private void registerCommands() {
        // 1. Inicializamos el framework de Lamp
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // 2. Le pedimos a Guice que nos construya los comandos con sus dependencias inyectadas
        handler.register(injector.getInstance(ComandoNexo.class));
        handler.register(injector.getInstance(ComandoVoid.class));

        // 3. 🌐 Comando Web Activado
        handler.register(injector.getInstance(WebCommand.class));
    }
}
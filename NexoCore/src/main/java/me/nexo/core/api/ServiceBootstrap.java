package me.nexo.core.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import me.nexo.core.NexoCore;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.NexoAPI;
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

import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.logging.Logger;

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

        // 🌟 FIX: 3.5. Despertamos la NexoAPI Global para que la variable estática se llene
        injector.getInstance(NexoAPI.class);

        // 4. Tareas en Segundo Plano (Ahora el HUD encontrará la API encendida)
        new HudTask(plugin).runTaskTimer(plugin, 20L, 20L);

        // 5. Hooks Externos
        if (server.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NexoExpansion(plugin).register();
        }

        // 6. 💡 PILAR 1: Registro de comandos NATIVO
        registerCommands();

        logger.info("¡Nexo Core V8.2: Core Purificado al 100% y API Web en línea!");
    }

    public void stopServices() {
        if (webServer != null) {
            webServer.stop();
        }

        // 🗄️ PILAR 4: Guardado Seguro Síncrono a través del Repositorio
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

        // 💉 PILAR 3: Usamos Guice para instanciar el PlayerListener
        pm.registerEvents(injector.getInstance(PlayerListener.class), plugin);

        // 💉 Inyectamos nuestro nuevo Listener purificado
        pm.registerEvents(injector.getInstance(me.nexo.core.menus.VoidBlessingMenuListener.class), plugin);

        // Eventos Legacy (Aún no purificados)
        pm.registerEvents(new me.nexo.core.listeners.VoidEssenceListener(plugin), plugin);
        pm.registerEvents(new me.nexo.core.hub.NexoMenuListener(plugin), plugin);
        pm.registerEvents(new me.nexo.core.menus.MenuGlobalListener(), plugin);
    }

    private void registerCommands() {
        try {
            // 🧠 Usamos Reflexión para acceder a la memoria de Bukkit y obtener el CommandMap real
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(server);

            // 💉 Le pedimos a Guice nuestras clases inyectadas y las registramos a la fuerza
            commandMap.register("nexo", injector.getInstance(ComandoNexo.class));
            commandMap.register("nexo", injector.getInstance(ComandoVoid.class));
            commandMap.register("nexo", injector.getInstance(WebCommand.class));

            logger.info("✅ Comandos Nativos inyectados con éxito (Zero-Lag)");
        } catch (Exception e) {
            logger.severe("❌ Error crítico inyectando comandos en el CommandMap: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
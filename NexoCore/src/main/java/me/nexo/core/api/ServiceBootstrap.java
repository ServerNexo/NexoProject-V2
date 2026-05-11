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
import me.nexo.core.visuals.MobVisualManager;

// 🌟 IMPORTACIONES FASE 2
import me.nexo.core.cataclysms.MeteorListener;
import me.nexo.core.hub.HubDonationGUI;
import me.nexo.core.commands.ComandoEventos;

// 🌟 IMPORTACIONES BOSSES Y EVENTOS
import me.nexo.core.commands.ComandoTestBoss;
import me.nexo.core.bosses.GlobalBossCombatListener;
import me.nexo.core.bosses.NexoBossRegistry; // 🌟 NUEVO IMPORT AÑADIDO
import me.nexo.core.events.NexoEventManager;
import me.nexo.core.events.types.BossInvasionEvent;
import me.nexo.core.crossplay.CrossplayUtils;

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

        databaseManager.conectar();
        webServer.start();
        registerEvents();

        injector.getInstance(HudTask.class).runTaskTimer(plugin, 20L, 20L);

        if (server.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            injector.getInstance(NexoExpansion.class).register();
        }

        registerCommands();

        // 7. 🌟 Arrancamos el Orquestador de Eventos Globales (Hilo Virtual)
        NexoEventManager eventManager = injector.getInstance(NexoEventManager.class);
        eventManager.iniciarMotor();

        // 🌟 REGISTRAMOS LA INVASIÓN PARA QUE EL ORQUESTADOR PUEDA USARLA
        eventManager.registrarEvento(
                new BossInvasionEvent(
                        plugin,
                        injector.getInstance(CrossplayUtils.class),
                        injector.getInstance(MobVisualManager.class),
                        injector.getInstance(NexoBossRegistry.class) // 🌟 FIX: EXTRAEMOS EL REGISTRY DE GUICE Y SE LO PASAMOS AL EVENTO
                )
        );

        logger.info("¡Nexo Core V8.2: Core Purificado al 100% y API Web en línea!");
    }

    public void stopServices() {
        injector.getInstance(NexoEventManager.class).apagarMotor();

        if (webServer != null) {
            webServer.stop();
        }

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

        pm.registerEvents(injector.getInstance(PlayerListener.class), plugin);
        pm.registerEvents(injector.getInstance(VoidBlessingMenuListener.class), plugin);
        pm.registerEvents(injector.getInstance(VoidEssenceListener.class), plugin);
        pm.registerEvents(injector.getInstance(NexoMenuListener.class), plugin);
        pm.registerEvents(injector.getInstance(MenuGlobalListener.class), plugin);
        pm.registerEvents(injector.getInstance(BedrockBugFixListener.class), plugin);
        pm.registerEvents(injector.getInstance(MobVisualManager.class), plugin);

        pm.registerEvents(injector.getInstance(MeteorListener.class), plugin);
        pm.registerEvents(injector.getInstance(HubDonationGUI.class), plugin);

        pm.registerEvents(injector.getInstance(GlobalBossCombatListener.class), plugin);
    }

    private void registerCommands() {
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        handler.register(injector.getInstance(ComandoNexo.class));
        handler.register(injector.getInstance(ComandoVoid.class));
        handler.register(injector.getInstance(WebCommand.class));
        handler.register(injector.getInstance(ComandoEventos.class));
        handler.register(injector.getInstance(ComandoTestBoss.class));
    }
}
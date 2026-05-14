package me.aeroxis.core.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.commands.*;
import me.aeroxis.core.database.DatabaseManager;
import me.aeroxis.core.events.AeroxisEventManager;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.user.UserRepository;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.config.ConfigManager;
import me.aeroxis.core.PlayerListener;
import me.aeroxis.core.HudTask;
import me.aeroxis.core.AeroxisExpansion;
import me.aeroxis.core.commands.ComandoAeroxis;
import me.aeroxis.core.crossplay.BedrockBugFixListener;
import me.aeroxis.core.hub.AeroxisMenuListener;
import me.aeroxis.core.listeners.VoidEssenceListener;
import me.aeroxis.core.menus.MenuGlobalListener;
import me.aeroxis.core.menus.VoidBlessingMenuListener;
import me.aeroxis.core.visuals.MobVisualManager;

// 🌟 IMPORTACIONES FASE 2
import me.aeroxis.core.cataclysms.MeteorListener;
import me.aeroxis.core.hub.HubDonationGUI;

// 🌟 IMPORTACIONES BOSSES Y EVENTOS
import me.aeroxis.core.bosses.GlobalBossCombatListener;
import me.aeroxis.core.bosses.AeroxisBossRegistry; // 🌟 NUEVO IMPORT AÑADIDO
import me.aeroxis.core.events.types.BossInvasionEvent;
import me.aeroxis.core.crossplay.CrossplayUtils;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.logging.Logger;

/**
 * 🏛️ Aeroxis Network - Service Bootstrap (Arquitectura Enterprise)
 * Orquestador central del ciclo de vida del plugin. Todo se enlaza a través de Guice.
 */
@Singleton
public class ServiceBootstrap {

    private final AeroxisCore plugin;
    private final Server server;
    private final Logger logger;
    private final Injector injector;

    private final DatabaseManager databaseManager;
    private final UserManager userManager;
    private final UserRepository userRepository;
    private final AeroxisWebServer webServer;
    private final ConfigManager configManager;

    @Inject
    public ServiceBootstrap(AeroxisCore plugin, Server server, Injector injector,
                            DatabaseManager databaseManager, UserManager userManager,
                            UserRepository userRepository, AeroxisWebServer webServer,
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
        logger.info("⚡ Arrancando Arquitectura Aeroxis Enterprise");
        logger.info("========================================");

        databaseManager.conectar();
        webServer.start();
        registerEvents();

        injector.getInstance(HudTask.class).runTaskTimer(plugin, 20L, 20L);

        if (server.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            injector.getInstance(AeroxisExpansion.class).register();
        }

        registerCommands();

        // 7. 🌟 Arrancamos el Orquestador de Eventos Globales (Hilo Virtual)
        AeroxisEventManager eventManager = injector.getInstance(AeroxisEventManager.class);
        eventManager.iniciarMotor();

        // 🌟 REGISTRAMOS LA INVASIÓN PARA QUE EL ORQUESTADOR PUEDA USARLA
        eventManager.registrarEvento(
                new BossInvasionEvent(
                        plugin,
                        injector.getInstance(CrossplayUtils.class),
                        injector.getInstance(MobVisualManager.class),
                        injector.getInstance(AeroxisBossRegistry.class) // 🌟 FIX: EXTRAEMOS EL REGISTRY DE GUICE Y SE LO PASAMOS AL EVENTO
                )
        );

        logger.info("¡Aeroxis Core V8.2: Core Purificado al 100% y API Web en línea!");
    }

    public void stopServices() {
        injector.getInstance(AeroxisEventManager.class).apagarMotor();

        if (webServer != null) {
            webServer.stop();
        }

        for (Player p : server.getOnlinePlayers()) {
            AeroxisUser user = userManager.getUserOrNull(p.getUniqueId());
            if (user != null) {
                userRepository.saveUserSync(user);
            }
        }

        if (databaseManager != null) {
            databaseManager.desconectar();
        }

        logger.info("AeroxisCore apagado y datos guardados de forma segura.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        pm.registerEvents(injector.getInstance(PlayerListener.class), plugin);
        pm.registerEvents(injector.getInstance(VoidBlessingMenuListener.class), plugin);
        pm.registerEvents(injector.getInstance(VoidEssenceListener.class), plugin);
        pm.registerEvents(injector.getInstance(AeroxisMenuListener.class), plugin);
        pm.registerEvents(injector.getInstance(MenuGlobalListener.class), plugin);
        pm.registerEvents(injector.getInstance(BedrockBugFixListener.class), plugin);
        pm.registerEvents(injector.getInstance(MobVisualManager.class), plugin);

        pm.registerEvents(injector.getInstance(MeteorListener.class), plugin);
        pm.registerEvents(injector.getInstance(HubDonationGUI.class), plugin);

        pm.registerEvents(injector.getInstance(GlobalBossCombatListener.class), plugin);
    }

    private void registerCommands() {
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        handler.register(injector.getInstance(ComandoAeroxis.class));
        handler.register(injector.getInstance(ComandoVoid.class));
        handler.register(injector.getInstance(WebCommand.class));
        handler.register(injector.getInstance(ComandoEventos.class));
        handler.register(injector.getInstance(ComandoTestBoss.class));
    }
}
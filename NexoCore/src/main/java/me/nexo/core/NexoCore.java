package me.nexo.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.di.NexoCoreModule;
import me.nexo.core.api.ServiceBootstrap;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * 🏛️ Nexo Network - Core Engine (Arquitectura Enterprise)
 * Motor central adaptado para Java 21, Paper 1.21.5 y Carga Segura Multi-Módulo con Guice.
 */
public final class NexoCore extends JavaPlugin {

    // 🛡️ Puente estático de transición. (La única variable static permitida en toda la arquitectura).
    private static NexoCore instance;

    private Injector injector;
    private ServiceBootstrap bootstrap;

    // 🌟 Sincronizador de arranque para submódulos (Evita Race Conditions de Bukkit)
    private CompletableFuture<Void> coreReadyFuture;

    // 🌟 CACHÉ DI: Puente de alta velocidad para módulos Legacy
    private DatabaseManager databaseManager;
    private UserManager userManager;
    private ConfigManager configManager;
    private UserRepository userRepository;

    @Override
    public void onLoad() {
        instance = this;
        this.coreReadyFuture = new CompletableFuture<>();

        getLogger().info("NexoCore: Inicializando entorno de pre-carga y Guice...");

        // 💉 Iniciamos el contenedor principal de Inyección de Dependencias
        this.injector = Guice.createInjector(new NexoCoreModule(this));
    }

    @Override
    public void onEnable() {
        getLogger().info("🚀 NexoCore: Arrancando servicios e inicializando Base de Datos...");

        // 🌟 Llenamos el caché desde el Injector UNA SOLA VEZ en el hilo principal
        this.databaseManager = injector.getInstance(DatabaseManager.class);
        this.userManager = injector.getInstance(UserManager.class);
        this.configManager = injector.getInstance(ConfigManager.class);
        this.userRepository = injector.getInstance(UserRepository.class);

        // Arrancamos el orquestador inyectado
        this.bootstrap = injector.getInstance(ServiceBootstrap.class);
        this.bootstrap.startServices();

        // 🔓 Liberamos la señal para que NexoEconomy, NexoWar, etc. puedan arrancar de forma segura
        this.coreReadyFuture.complete(null);
        getLogger().info("✅ NexoCore listo. Submódulos autorizados para arrancar.");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // ==========================================================
    // 🛡️ BYPASS NATIVO PAPER 1.21.5 (ELIMINA EL CRASHEO DE LAMP)
    // ==========================================================
    /**
     * Paper bloquea JavaPlugin#getCommand cuando se usa paper-plugin.yml.
     * Al interceptar este método y devolver null, forzamos a Revxrsal Lamp
     * a registrar los comandos directamente en el CommandMap usando reflexión,
     * saltándose la seguridad de Paper sin causar excepciones.
     */
    @Override
    public PluginCommand getCommand(@NotNull String name) {
        return null; // ¡Magia pura!
    }

    // ==========================================================
    // 🌐 MÉTODOS DE ARQUITECTURA Y SINCRONIZACIÓN
    // ==========================================================

    /**
     * @return CompletableFuture que se completa cuando NexoCore ha finalizado su onEnable.
     */
    public CompletableFuture<Void> getCoreReadyFuture() {
        return coreReadyFuture;
    }

    /**
     * 💉 FUNDAMENTAL: Permite a submódulos crear Inyectores Hijos (ChildInjectors)
     * para compartir los mismos Managers y Repositorios del Core.
     */
    public Injector getInjector() {
        return this.injector;
    }

    // ==========================================================
    // 🌉 PUENTE LEGACY (Soporte temporal para NexoWar, NexoPvP, etc.)
    // ==========================================================

    @Deprecated
    public static NexoCore getInstance() {
        return instance;
    }

    @Deprecated
    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    @Deprecated
    public UserManager getUserManager() {
        return this.userManager;
    }

    @Deprecated
    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    @Deprecated
    public UserRepository getUserRepository() {
        return this.userRepository;
    }
}
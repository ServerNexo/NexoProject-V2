package me.nexo.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.di.NexoCoreModule;
import me.nexo.core.api.ServiceBootstrap;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.user.UserManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * 🏛️ Nexo Network - Core Engine (Enterprise Architecture)
 */
public final class NexoCore extends JavaPlugin {

    // 🌟 Variable estática para acceso global seguro
    private static NexoCore instance;

    private Injector injector;
    private ServiceBootstrap bootstrap;

    // 🌟 LA PROMESA: El candado que detendrá a los submódulos hasta que el Core termine
    private CompletableFuture<Void> coreReadyFuture;

    @Override
    public void onLoad() {
        instance = this;
        this.coreReadyFuture = new CompletableFuture<>(); // 🔒 Creamos el candado cerrado

        getLogger().info("NexoCore: Inicializando entorno de pre-carga y Guice...");
        this.injector = Guice.createInjector(new NexoCoreModule(this));
    }

    @Override
    public void onEnable() {
        getLogger().info("🚀 NexoCore: Arrancando servicios e inicializando Base de Datos...");

        this.bootstrap = injector.getInstance(ServiceBootstrap.class);
        this.bootstrap.startServices(); // Aquí arranca tu DatabaseManager y demás

        // 🔓 ABRIMOS EL CANDADO: Notificamos a todos los submódulos que ya pueden inyectar y usar la BD
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
    // 🌟 API MODERNA (Para evitar Condiciones de Carrera)
    // ==========================================================

    public static NexoCore getInstance() {
        return instance;
    }

    /**
     * Los submódulos deben llamar a este método en su onEnable() usando .thenRun(() -> { ... })
     */
    public CompletableFuture<Void> getCoreReadyFuture() {
        return coreReadyFuture;
    }

    // ==========================================================
    // 🌉 PUENTE LEGACY (Soporte temporal para NexoWar, NexoPvP, etc.)
    // Estos métodos mantendrán vivos a los módulos viejos hasta
    // que los refactoricemos con Inyección de Dependencias.
    // ==========================================================

    @Deprecated
    public DatabaseManager getDatabaseManager() {
        return injector.getInstance(DatabaseManager.class);
    }

    @Deprecated
    public UserManager getUserManager() {
        return injector.getInstance(UserManager.class);
    }

    @Deprecated
    public ConfigManager getConfigManager() {
        return injector.getInstance(ConfigManager.class);
    }

    @Deprecated
    public me.nexo.core.user.UserRepository getUserRepository() {
        return injector.getInstance(me.nexo.core.user.UserRepository.class);
    }
}
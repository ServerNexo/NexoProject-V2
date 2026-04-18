package me.nexo.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.di.NexoCoreModule;
import me.nexo.core.api.ServiceBootstrap;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.user.UserManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 🏛️ Nexo Network - Core Engine (Enterprise Architecture)
 */
public final class NexoCore extends JavaPlugin {

    private Injector injector;
    private ServiceBootstrap bootstrap;

    @Override
    public void onLoad() {
        getLogger().info("NexoCore: Inicializando entorno de pre-carga y Guice...");
        this.injector = Guice.createInjector(new NexoCoreModule(this));
    }

    @Override
    public void onEnable() {
        this.bootstrap = injector.getInstance(ServiceBootstrap.class);
        this.bootstrap.startServices();
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
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
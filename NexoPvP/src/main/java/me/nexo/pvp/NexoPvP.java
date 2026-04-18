package me.nexo.pvp;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.pvp.api.PvPBootstrap;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.di.PvPModule;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ⚔️ NexoPvP - Núcleo de Combate (Arquitectura Enterprise)
 */
public class NexoPvP extends JavaPlugin {

    private Injector injector;
    private PvPBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");

        // 💉 Iniciamos Guice y el Módulo PvP
        this.injector = Guice.createInjector(new PvPModule(this));
        this.bootstrap = injector.getInstance(PvPBootstrap.class);

        // 🚀 Encendemos todos los servicios
        this.bootstrap.startServices();

        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.stopServices();
        }
    }

    // 🌉 PUENTE LEGACY (Para las clases que aún no purificamos)
    @Deprecated
    public ConfigManager getConfigManager() {
        return injector.getInstance(ConfigManager.class);
    }
}
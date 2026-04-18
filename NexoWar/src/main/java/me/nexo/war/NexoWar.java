package me.nexo.war;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.war.config.ConfigManager;
import me.nexo.war.di.WarModule;
import me.nexo.war.managers.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoWar extends JavaPlugin {

    private Injector injector;
    private WarBootstrap bootstrap;

    @Override
    public void onEnable() {
        // 💉 Inicializar el motor de Inyección (Guice)
        this.injector = Guice.createInjector(new WarModule(this));

        // 🚀 Arrancar el Orquestador (Bootstrap)
        this.bootstrap = injector.getInstance(WarBootstrap.class);
        this.bootstrap.startServices();
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // 🌟 Mantenemos los getters extrayéndolos del Injector por si otras APIs los necesitan
    public WarManager getWarManager() {
        return injector.getInstance(WarManager.class);
    }

    public ConfigManager getConfigManager() {
        return injector.getInstance(ConfigManager.class);
    }
}
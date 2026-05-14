package me.aeroxis.crates;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.aeroxis.crates.di.CratesModule;
import org.bukkit.plugin.java.JavaPlugin;

public class AeroxisCrates extends JavaPlugin {

    private Injector injector;
    private CratesBootstrap bootstrap;

    @Override
    public void onEnable() {
        // Inicializamos Guice
        this.injector = Guice.createInjector(new CratesModule(this));
        
        // Arrancamos el Bootstrap
        this.bootstrap = injector.getInstance(CratesBootstrap.class);
        this.bootstrap.startServices();
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    public Injector getInjector() {
        return injector;
    }
}
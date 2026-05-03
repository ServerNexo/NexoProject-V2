package me.nexo.tools;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.tools.di.ToolsModule;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoTools extends JavaPlugin {

    private Injector injector;

    @Override
    public void onEnable() {
        // Inicializamos el contenedor de dependencias (Guice)
        this.injector = Guice.createInjector(new ToolsModule(this));
        
        // Arrancamos el Bootstrap de comandos y managers
        injector.getInstance(ToolsBootstrap.class).init();
        
        getLogger().info("✅ NexoTools (Essentials) activado y vinculado al Core.");
    }

    @Override
    public void onDisable() {
        getLogger().info("❌ NexoTools desactivado.");
    }

    public Injector getInjector() {
        return injector;
    }
}
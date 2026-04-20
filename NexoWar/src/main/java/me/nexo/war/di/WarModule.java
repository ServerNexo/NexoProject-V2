package me.nexo.war.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.user.UserManager;
import me.nexo.war.NexoWar;

/**
 * 💉 NexoWar - Módulo de Inyección de Dependencias
 */
public class WarModule extends AbstractModule {

    private final NexoWar plugin;

    public WarModule(NexoWar plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Le enseñamos a Guice quién es el plugin local
        bind(NexoWar.class).toInstance(plugin);
    }

    // 🌟 FIX: Usamos el Singleton seguro (getInstance) en lugar del getPlugin de Bukkit
    @Provides
    @Singleton
    public NexoCore provideNexoCore() {
        return NexoCore.getInstance();
    }

    // 🌟 FIX: Obtenemos el UserManager desde la instancia global asegurada
    @Provides
    @Singleton
    public UserManager provideUserManager() {
        return NexoCore.getInstance().getUserManager();
    }
}
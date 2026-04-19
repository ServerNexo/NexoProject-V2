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

        // ❌ FIX: Eliminamos las llamadas estáticas de Bukkit de aquí
    }

    // 🌟 FIX: "Lazy Loading". Guice solo ejecutará esto cuando alguien necesite a NexoCore
    @Provides
    @Singleton
    public NexoCore provideNexoCore() {
        return NexoCore.getPlugin(NexoCore.class);
    }

    // 🌟 FIX: "Lazy Loading" para el UserManager
    @Provides
    @Singleton
    public UserManager provideUserManager() {
        return NexoCore.getPlugin(NexoCore.class).getUserManager();
    }
}
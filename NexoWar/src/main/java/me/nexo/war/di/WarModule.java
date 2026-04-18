package me.nexo.war.di;

import com.google.inject.AbstractModule;
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
        // Le enseñamos a Guice quién es el plugin y el núcleo
        bind(NexoWar.class).toInstance(plugin);

        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        bind(NexoCore.class).toInstance(core);
        bind(UserManager.class).toInstance(core.getUserManager());
    }
}
package me.nexo.factories.di;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;

/**
 * 🏭 NexoFactories - Módulo de Inyección de Dependencias (Guice)
 */
public class FactoriesModule extends AbstractModule {

    private final NexoFactories plugin;

    public FactoriesModule(NexoFactories plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia principal del plugin
        bind(NexoFactories.class).toInstance(plugin);

        // Registramos los Managers como Singletons (1 sola instancia en todo el server)
        bind(FactoryManager.class).in(Scopes.SINGLETON);
        bind(BlueprintManager.class).in(Scopes.SINGLETON);

        // Registramos Listeners y Comandos
        bind(FactoryInteractListener.class).in(Scopes.SINGLETON);
        bind(ComandoFactory.class).in(Scopes.SINGLETON);
    }
}
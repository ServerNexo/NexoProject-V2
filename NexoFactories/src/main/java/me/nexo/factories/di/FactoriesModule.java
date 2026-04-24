package me.nexo.factories.di;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.logic.ScriptEvaluator;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;

/**
 * 🏭 NexoFactories - Módulo de Inyección de Dependencias (Arquitectura Enterprise Java 21)
 * Rendimiento: Herencia Automática de Inyector Padre (NexoCore) y Bindings Explícitos.
 */
public class FactoriesModule extends AbstractModule {

    private final NexoFactories plugin;

    // 💉 PILAR 1: Solo pedimos el plugin local. Las dependencias del Core (Database, Utils)
    // se heredan automáticamente gracias al 'createChildInjector' en la clase principal.
    public FactoriesModule(NexoFactories plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia principal del plugin local
        bind(NexoFactories.class).toInstance(plugin);

        // Registramos los motores lógicos e I/O como Singletons puros
        bind(FactoryManager.class).in(Scopes.SINGLETON);
        bind(BlueprintManager.class).in(Scopes.SINGLETON);
        bind(ScriptEvaluator.class).in(Scopes.SINGLETON);

        // Registramos Listeners y Comandos
        bind(FactoryInteractListener.class).in(Scopes.SINGLETON);
        bind(ComandoFactory.class).in(Scopes.SINGLETON);
    }
}
package me.nexo.factories.di;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import me.nexo.core.NexoCore;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.logic.ScriptEvaluator;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;

/**
 * 🏭 NexoFactories - Módulo de Inyección de Dependencias (Arquitectura Enterprise Java 21)
 * Rendimiento: Puente Inter-Modular Estricto y Bindings Explícitos.
 */
public class FactoriesModule extends AbstractModule {

    private final NexoFactories plugin;
    private final NexoCore corePlugin;

    // 💉 PILAR 1: Exigimos las instancias base desde el Bootstrap
    public FactoriesModule(NexoFactories plugin, NexoCore corePlugin) {
        this.plugin = plugin;
        this.corePlugin = corePlugin;
    }

    @Override
    protected void configure() {
        // Enlazamos las instancias principales de los plugins
        bind(NexoFactories.class).toInstance(plugin);
        bind(NexoCore.class).toInstance(corePlugin);

        /* * 💡 NOTA DEL ARQUITECTO:
         * Aquí es donde construimos el puente con el Core. Descomenta y ajusta 
         * estas líneas según los getters que tengas en tu clase NexoCore 
         * para que Guice pueda inyectarlos en FactoryManager y BlueprintManager.
         * * bind(me.nexo.core.database.DatabaseManager.class).toInstance(corePlugin.getDatabaseManager());
         * bind(me.nexo.core.crossplay.CrossplayUtils.class).toInstance(corePlugin.getCrossplayUtils());
         * bind(me.nexo.core.user.UserManager.class).toInstance(corePlugin.getUserManager());
         */

        // Registramos los motores lógicos e I/O como Singletons puros
        bind(FactoryManager.class).in(Scopes.SINGLETON);
        bind(BlueprintManager.class).in(Scopes.SINGLETON);
        bind(ScriptEvaluator.class).in(Scopes.SINGLETON);

        // Registramos Listeners y Comandos
        bind(FactoryInteractListener.class).in(Scopes.SINGLETON);
        bind(ComandoFactory.class).in(Scopes.SINGLETON);
    }
}
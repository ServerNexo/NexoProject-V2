package me.nexo.factories.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.factories.NexoFactories;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.logic.ScriptEvaluator;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.nexo.protections.NexoProtections;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;

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

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    /**
     * Puente hacia Protecciones: Para verificar permisos al colocar fábricas.
     */
    @Provides
    @Singleton
    public ClaimManager proveerClaimManager() {
        NexoProtections protPlugin = JavaPlugin.getPlugin(NexoProtections.class);
        return protPlugin.getChildInjector().getInstance(ClaimManager.class);
    }

    /**
     * Puente hacia Ítems: Para manejar los blueprints y materiales de las fábricas.
     */
    @Provides
    @Singleton
    public ItemManager proveerItemManager() {
        NexoItems itemsPlugin = JavaPlugin.getPlugin(NexoItems.class);
        return itemsPlugin.getChildInjector().getInstance(ItemManager.class);
    }
}
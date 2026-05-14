package me.aeroxis.factories.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import me.aeroxis.factories.AeroxisFactories;
import me.aeroxis.items.AeroxisItems;
import org.bukkit.Bukkit;

import me.aeroxis.factories.commands.ComandoFactory;
import me.aeroxis.factories.listeners.FactoryInteractListener;
import me.aeroxis.factories.logic.ScriptEvaluator;
import me.aeroxis.factories.managers.BlueprintManager;
import me.aeroxis.factories.managers.FactoryManager;

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.aeroxis.protections.AeroxisProtections;
import me.aeroxis.protections.managers.ClaimManager;
import me.aeroxis.items.managers.ItemManager;

/**
 * 🏭 AeroxisFactories - Módulo de Inyección de Dependencias (Arquitectura Enterprise Java 21)
 * Rendimiento: Herencia Automática de Inyector Padre (AeroxisCore) y Bindings Explícitos.
 */
public class FactoriesModule extends AbstractModule {

    private final AeroxisFactories plugin;

    // 💉 PILAR 1: Solo pedimos el plugin local. Las dependencias del Core (Database, Utils)
    // se heredan automáticamente gracias al 'createChildInjector' en la clase principal.
    public FactoriesModule(AeroxisFactories plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia principal del plugin local
        bind(AeroxisFactories.class).toInstance(plugin);

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
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el nuevo nombre getInjector()
        AeroxisProtections protPlugin = (AeroxisProtections) Bukkit.getPluginManager().getPlugin("AeroxisProtections");
        if (protPlugin != null && protPlugin.getInjector() != null) {
            return protPlugin.getInjector().getInstance(ClaimManager.class);
        }
        return null;
    }

    /**
     * Puente hacia Ítems: Para manejar los blueprints y materiales de las fábricas.
     */
    @Provides
    @Singleton
    public ItemManager proveerItemManager() {
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el nuevo nombre getInjector()
        AeroxisItems itemsPlugin = (AeroxisItems) Bukkit.getPluginManager().getPlugin("AeroxisItems");
        if (itemsPlugin != null && itemsPlugin.getInjector() != null) {
            return itemsPlugin.getInjector().getInstance(ItemManager.class);
        }
        return null;
    }
}
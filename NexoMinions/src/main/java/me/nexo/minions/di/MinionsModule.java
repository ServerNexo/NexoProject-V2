package me.nexo.minions.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.minions.NexoMinions;
import me.nexo.minions.MinionsBootstrap;
import me.nexo.minions.commands.ComandoMinion;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.TiersConfig;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.listeners.ExplosionListener;
import me.nexo.minions.listeners.MinionListener;
import me.nexo.minions.listeners.MinionLoadListener;
import me.nexo.minions.manager.MinionManager;

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.managers.ClaimManager;

/**
 * 💉 NexoMinions - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) para prevenir Lag Spikes (Micro-tirones).
 * Nota: NexoCore, DatabaseManager y CrossplayUtils ya se heredan automáticamente del Inyector Padre.
 */
public class MinionsModule extends AbstractModule {

    private final NexoMinions plugin;

    public MinionsModule(NexoMinions plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia del plugin
        bind(NexoMinions.class).toInstance(plugin);

        // ==========================================
        // 🚀 ORQUESTADOR
        // ==========================================
        bind(MinionsBootstrap.class).asEagerSingleton();

        // ==========================================
        // 📂 CONFIGURACIONES (YAML)
        // ==========================================
        bind(ConfigManager.class).asEagerSingleton();
        bind(TiersConfig.class).asEagerSingleton();
        bind(UpgradesConfig.class).asEagerSingleton();

        // ==========================================
        // 🧠 CEREBROS (MANAGERS)
        // ==========================================
        bind(MinionManager.class).asEagerSingleton();

        // ==========================================
        // 🎧 EVENTOS (LISTENERS)
        // ==========================================
        bind(MinionListener.class).asEagerSingleton();
        bind(MinionLoadListener.class).asEagerSingleton();
        bind(ExplosionListener.class).asEagerSingleton();

        // ==========================================
        // ⌨️ COMANDOS (REVXRSAL LAMP)
        // ==========================================
        bind(ComandoMinion.class).asEagerSingleton();
    }

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    /**
     * Puente hacia Colecciones: Para sumar progreso de farmeo de los Minions.
     */
    @Provides
    @Singleton
    public CollectionManager proveerCollectionManager() {
        NexoColecciones colPlugin = JavaPlugin.getPlugin(NexoColecciones.class);
        return colPlugin.getChildInjector().getInstance(CollectionManager.class);
    }

    /**
     * Puente hacia Protecciones: Para validar límites o permisos en claims.
     */
    @Provides
    @Singleton
    public ClaimManager proveerClaimManager() {
        NexoProtections protPlugin = JavaPlugin.getPlugin(NexoProtections.class);
        return protPlugin.getChildInjector().getInstance(ClaimManager.class);
    }
}
package me.nexo.colecciones.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.FlushTask;
import me.nexo.colecciones.commands.ComandoColecciones;
import me.nexo.colecciones.commands.ComandoSlayer;
import me.nexo.colecciones.config.ConfigManager;
import me.nexo.colecciones.slayers.SlayerListener;
import me.nexo.colecciones.slayers.SlayerManager;

// 🌟 IMPORTACIONES DEL PUENTE HORIZONTAL
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;

/**
 * 📚 NexoColecciones - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga en memoria instantánea (Eager Singleton) para prevenir tirones de Lag.
 * Nota: No es necesario enlazar NexoCore ni sus managers aquí, ya los hereda del Inyector Padre.
 */
public class ColeccionesModule extends AbstractModule {

    private final NexoColecciones plugin;

    public ColeccionesModule(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia del plugin
        bind(NexoColecciones.class).toInstance(plugin);

        // ==========================================
        // 🚀 CONFIGURACIONES GLOBALES
        // ==========================================
        bind(ConfigManager.class).asEagerSingleton();
        bind(ColeccionesConfig.class).asEagerSingleton();

        // ==========================================
        // 🧠 CEREBROS (MANAGERS)
        // ==========================================
        bind(CollectionManager.class).asEagerSingleton();
        bind(SlayerManager.class).asEagerSingleton();

        // ==========================================
        // 🎧 EVENTOS Y TAREAS (LISTENERS & TASKS)
        // ==========================================
        bind(ColeccionesListener.class).asEagerSingleton();
        bind(SlayerListener.class).asEagerSingleton();
        bind(FlushTask.class).asEagerSingleton();

        // ==========================================
        // ⌨️ COMANDOS NATIVOS
        // ==========================================
        bind(ComandoColecciones.class).asEagerSingleton();
        bind(ComandoSlayer.class).asEagerSingleton();
    }

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    /**
     * Le decimos a Guice explícitamente de dónde sacar el EconomyManager
     * para que NO intente hacer "new NexoEconomy()".
     */
    @Provides
    @Singleton
    public EconomyManager proveerEconomyManager() {
        // 1. Buscamos la instancia oficial que PaperMC ya cargó
        NexoEconomy ecoPlugin = JavaPlugin.getPlugin(NexoEconomy.class);

        // 2. Extraemos su manager del inyector hijo para compartir la misma memoria
        return ecoPlugin.getChildInjector().getInstance(EconomyManager.class);
    }
}
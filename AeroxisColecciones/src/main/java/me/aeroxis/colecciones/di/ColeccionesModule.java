package me.aeroxis.colecciones.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.colecciones.AeroxisColecciones;
import me.aeroxis.economy.AeroxisEconomy;
import org.bukkit.Bukkit; // 🌟 AÑADIDO: Importación segura de Bukkit

import me.aeroxis.colecciones.colecciones.ColeccionesConfig;
import me.aeroxis.colecciones.colecciones.ColeccionesListener;
import me.aeroxis.colecciones.colecciones.CollectionManager;
import me.aeroxis.colecciones.colecciones.FlushTask;
import me.aeroxis.colecciones.commands.ComandoColecciones;
import me.aeroxis.colecciones.commands.ComandoSlayer;
import me.aeroxis.colecciones.config.ConfigManager;
import me.aeroxis.colecciones.slayers.SlayerListener;
import me.aeroxis.colecciones.slayers.SlayerManager;

// 🌟 IMPORTACIONES DEL PUENTE HORIZONTAL
import me.aeroxis.economy.core.EconomyManager;

/**
 * 📚 AeroxisColecciones - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga en memoria instantánea (Eager Singleton) para prevenir tirones de Lag.
 * Nota: No es necesario enlazar AeroxisCore ni sus managers aquí, ya los hereda del Inyector Padre.
 */
public class ColeccionesModule extends AbstractModule {

    private final AeroxisColecciones plugin;

    public ColeccionesModule(AeroxisColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia del plugin
        bind(AeroxisColecciones.class).toInstance(plugin);

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
     * para que NO intente hacer "new AeroxisEconomy()".
     */
    @Provides
    @Singleton
    public EconomyManager proveerEconomyManager() {
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el método estandarizado getInjector()
        AeroxisEconomy ecoPlugin = (AeroxisEconomy) Bukkit.getPluginManager().getPlugin("AeroxisEconomy");
        if (ecoPlugin != null && ecoPlugin.getInjector() != null) {
            return ecoPlugin.getInjector().getInstance(EconomyManager.class);
        }
        return null;
    }
}
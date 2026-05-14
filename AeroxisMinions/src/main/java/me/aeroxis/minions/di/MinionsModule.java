package me.aeroxis.minions.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.colecciones.AeroxisColecciones;
import me.aeroxis.islas.AeroxisIslas;
import org.bukkit.Bukkit;

import me.aeroxis.minions.AeroxisMinions;
import me.aeroxis.minions.MinionsBootstrap;
import me.aeroxis.minions.commands.ComandoMinion;
import me.aeroxis.minions.config.ConfigManager;
import me.aeroxis.minions.data.TiersConfig;
import me.aeroxis.minions.data.UpgradesConfig;
import me.aeroxis.minions.listeners.ExplosionListener;
import me.aeroxis.minions.listeners.MinionListener;
import me.aeroxis.minions.listeners.MinionLoadListener;
import me.aeroxis.minions.manager.MinionManager;

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.aeroxis.colecciones.colecciones.CollectionManager;
import me.aeroxis.protections.AeroxisProtections;
import me.aeroxis.protections.managers.ClaimManager;
import me.aeroxis.islas.managers.IslandManager;
import me.aeroxis.islas.managers.IslandLevelEngine; // 🌟 AÑADIDO: Faltaba para el MinionManager

/**
 * 💉 AeroxisMinions - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) para prevenir Lag Spikes (Micro-tirones).
 * Nota: AeroxisCore, DatabaseManager y CrossplayUtils ya se heredan automáticamente del Inyector Padre.
 */
public class MinionsModule extends AbstractModule {

    private final AeroxisMinions plugin;

    public MinionsModule(AeroxisMinions plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia del plugin
        bind(AeroxisMinions.class).toInstance(plugin);

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
    // 🌟 FIX: Usamos Bukkit.getPluginManager() en lugar de JavaPlugin.getPlugin()
    // para evitar el crash de "Plugin already initialized".

    /**
     * Puente hacia Colecciones: Para sumar progreso de farmeo de los Minions.
     */
    @Provides
    @Singleton
    public CollectionManager proveerCollectionManager() {
        AeroxisColecciones colPlugin = (AeroxisColecciones) Bukkit.getPluginManager().getPlugin("AeroxisColecciones");
        if (colPlugin != null && colPlugin.getInjector() != null) {
            return colPlugin.getInjector().getInstance(CollectionManager.class);
        }
        return null;
    }

    /**
     * Puente hacia Protecciones: Para validar límites o permisos en claims.
     */
    @Provides
    @Singleton
    public ClaimManager proveerClaimManager() {
        AeroxisProtections protPlugin = (AeroxisProtections) Bukkit.getPluginManager().getPlugin("AeroxisProtections");
        if (protPlugin != null && protPlugin.getInjector() != null) {
            return protPlugin.getInjector().getInstance(ClaimManager.class);
        }
        return null;
    }

    /**
     * Puente hacia Islas (Manager): Para gestionar datos base de las islas.
     */
    @Provides
    @Singleton
    public IslandManager proveerIslandManager() {
        AeroxisIslas islasPlugin = (AeroxisIslas) Bukkit.getPluginManager().getPlugin("AeroxisIslas");
        if (islasPlugin != null && islasPlugin.getInjector() != null) {
            return islasPlugin.getInjector().getInstance(IslandManager.class);
        }
        return null;
    }

    /**
     * 🌟 NUEVO PUENTE: Hacia Islas (Engine): Para inyectar el Diezmo (25%) de XP.
     * (Requerido por MinionManager línea 58).
     */
    @Provides
    @Singleton
    public IslandLevelEngine proveerIslandLevelEngine() {
        AeroxisIslas islasPlugin = (AeroxisIslas) Bukkit.getPluginManager().getPlugin("AeroxisIslas");
        if (islasPlugin != null && islasPlugin.getInjector() != null) {
            return islasPlugin.getInjector().getInstance(IslandLevelEngine.class);
        }
        return null;
    }
}
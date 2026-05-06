package me.nexo.minions.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;

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
import me.nexo.islas.NexoIslas;
import me.nexo.islas.managers.IslandManager;
import me.nexo.islas.managers.IslandLevelEngine; // 🌟 AÑADIDO: Faltaba para el MinionManager

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
    // 🌟 FIX: Usamos Bukkit.getPluginManager() en lugar de JavaPlugin.getPlugin()
    // para evitar el crash de "Plugin already initialized".

    /**
     * Puente hacia Colecciones: Para sumar progreso de farmeo de los Minions.
     */
    @Provides
    @Singleton
    public CollectionManager proveerCollectionManager() {
        NexoColecciones colPlugin = (NexoColecciones) Bukkit.getPluginManager().getPlugin("NexoColecciones");
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
        NexoProtections protPlugin = (NexoProtections) Bukkit.getPluginManager().getPlugin("NexoProtections");
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
        NexoIslas islasPlugin = (NexoIslas) Bukkit.getPluginManager().getPlugin("NexoIslas");
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
        NexoIslas islasPlugin = (NexoIslas) Bukkit.getPluginManager().getPlugin("NexoIslas");
        if (islasPlugin != null && islasPlugin.getInjector() != null) {
            return islasPlugin.getInjector().getInstance(IslandLevelEngine.class);
        }
        return null;
    }
}
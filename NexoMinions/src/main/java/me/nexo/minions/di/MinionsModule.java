package me.nexo.minions.di;

import com.google.inject.AbstractModule;
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
}
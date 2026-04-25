package me.nexo.pvp.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.api.PvPBootstrap;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.pasivas.PasivasManager;
import me.nexo.pvp.pvp.PvPManager;

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.managers.ClaimManager;

/**
 * 💉 NexoPvP - Módulo de Inyección de Dependencias (Arquitectura Enterprise)
 * Configura los enlaces específicos del submódulo de combate.
 * Nota: Los servicios del Core (Repositorios, Economía, Utils) se heredan automáticamente del CoreInjector.
 */
public class PvPModule extends AbstractModule {

    private final NexoPvP plugin;

    public PvPModule(NexoPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // 🛡️ Vinculamos la instancia actual del plugin de PvP
        bind(NexoPvP.class).toInstance(plugin);

        // ⚔️ REGISTRO DE COMPONENTES DE COMBATE Y PASIVAS
        // Al usar 'asEagerSingleton', Guice los instancia al arrancar el plugin
        // evitando tirones de lag (lazy-loading) durante el primer golpe de una pelea.
        bind(ConfigManager.class).asEagerSingleton();
        bind(PvPManager.class).asEagerSingleton();
        bind(PasivasManager.class).asEagerSingleton();

        // 🚀 Orquestador principal
        bind(PvPBootstrap.class).asEagerSingleton();
    }

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    /**
     * Puente hacia la Economía: Previene el "Plugin already initialized!"
     */
    @Provides
    @Singleton
    public EconomyManager proveerEconomyManager() {
        NexoEconomy ecoPlugin = JavaPlugin.getPlugin(NexoEconomy.class);
        return ecoPlugin.getChildInjector().getInstance(EconomyManager.class);
    }

    /**
     * Puente hacia las Protecciones: Previene el "Plugin already initialized!"
     */
    @Provides
    @Singleton
    public ClaimManager proveerClaimManager() {
        NexoProtections protPlugin = JavaPlugin.getPlugin(NexoProtections.class);
        return protPlugin.getChildInjector().getInstance(ClaimManager.class);
    }
}
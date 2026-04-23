package me.nexo.pvp.di;

import com.google.inject.AbstractModule;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.api.PvPBootstrap;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.pasivas.PasivasManager;
import me.nexo.pvp.pvp.PvPManager;

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
}
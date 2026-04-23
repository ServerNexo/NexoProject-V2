package me.nexo.clans.di;

import com.google.inject.AbstractModule;
import me.nexo.clans.NexoClans;
import me.nexo.clans.commands.ComandoChatClan;
import me.nexo.clans.commands.ComandoClan;
import me.nexo.clans.config.ConfigManager;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.listeners.ClanDamageListener;

/**
 * 👥 NexoClans - Módulo de Inyección de Dependencias (Guice)
 * Rendimiento: Carga en memoria instantánea (Eager Singleton) para prevenir tirones de Lag.
 */
public class ClansModule extends AbstractModule {

    private final NexoClans plugin;

    public ClansModule(NexoClans plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Core y Configuración
        bind(NexoClans.class).toInstance(plugin);
        
        // 🌟 FIX: asEagerSingleton() obliga a construir el objeto en el arranque (onEnable), 
        // evitando el "Lazy Loading" que causa lag cuando el primer jugador desencadena el evento.
        bind(ConfigManager.class).asEagerSingleton();

        // Cerebros (Managers)
        bind(ClanManager.class).asEagerSingleton();

        // Eventos
        bind(ClanConnectionListener.class).asEagerSingleton();
        bind(ClanDamageListener.class).asEagerSingleton();

        // Comandos
        bind(ComandoClan.class).asEagerSingleton();
        bind(ComandoChatClan.class).asEagerSingleton();
    }
}
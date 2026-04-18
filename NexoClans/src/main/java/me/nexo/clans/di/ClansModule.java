package me.nexo.clans.di;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import me.nexo.clans.NexoClans;
import me.nexo.clans.commands.ComandoChatClan;
import me.nexo.clans.commands.ComandoClan;
import me.nexo.clans.config.ConfigManager;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.listeners.ClanDamageListener;

/**
 * 👥 NexoClans - Módulo de Inyección de Dependencias (Guice)
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
        bind(ConfigManager.class).in(Scopes.SINGLETON);

        // Cerebros (Managers)
        bind(ClanManager.class).in(Scopes.SINGLETON);

        // Eventos
        bind(ClanConnectionListener.class).in(Scopes.SINGLETON);
        bind(ClanDamageListener.class).in(Scopes.SINGLETON);

        // Comandos
        bind(ComandoClan.class).in(Scopes.SINGLETON);
        bind(ComandoChatClan.class).in(Scopes.SINGLETON);
    }
}
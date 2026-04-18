package me.nexo.economy.di;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.bazar.BazaarChatListener;
import me.nexo.economy.bazar.BazaarManager;
import me.nexo.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.commands.*;
import me.nexo.economy.config.ConfigManager;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.listeners.EconomyListener;
import me.nexo.economy.listeners.TradeListener;
import me.nexo.economy.trade.TradeManager;

/**
 * 💰 NexoEconomy - Módulo de Inyección de Dependencias (Guice)
 */
public class EconomyModule extends AbstractModule {

    private final NexoEconomy plugin;

    public EconomyModule(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Plugin Principal
        bind(NexoEconomy.class).toInstance(plugin);

        // Configuración
        bind(ConfigManager.class).in(Scopes.SINGLETON);

        // Managers (Cerebros)
        bind(EconomyManager.class).in(Scopes.SINGLETON);
        bind(BazaarManager.class).in(Scopes.SINGLETON);
        bind(TradeManager.class).in(Scopes.SINGLETON);
        bind(BlackMarketManager.class).in(Scopes.SINGLETON);

        // Listeners
        bind(EconomyListener.class).in(Scopes.SINGLETON);
        bind(TradeListener.class).in(Scopes.SINGLETON);
        bind(BazaarChatListener.class).in(Scopes.SINGLETON);

        // Comandos
        bind(ComandoEco.class).in(Scopes.SINGLETON);
        bind(ComandoBazar.class).in(Scopes.SINGLETON);
        bind(ComandoTrade.class).in(Scopes.SINGLETON);
        bind(ComandoMercadoNegro.class).in(Scopes.SINGLETON);
    }
}
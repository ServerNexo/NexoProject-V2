package me.nexo.economy.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;

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

// 🌟 FIX CRÍTICO: Importamos las clases de NexoItems
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;

/**
 * 💰 NexoEconomy - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) para prevenir Lag Spikes en Gameplay.
 */
public class EconomyModule extends AbstractModule {

    private final NexoEconomy plugin;

    public EconomyModule(NexoEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // ==========================================
        // ⚙️ CORE
        // ==========================================
        bind(NexoEconomy.class).toInstance(plugin);
        bind(ConfigManager.class).asEagerSingleton();

        // ==========================================
        // 🧠 MANAGERS (Cerebros)
        // ==========================================
        bind(EconomyManager.class).asEagerSingleton();
        bind(BazaarManager.class).asEagerSingleton();
        bind(TradeManager.class).asEagerSingleton();
        bind(BlackMarketManager.class).asEagerSingleton();

        // ==========================================
        // 🛡️ LISTENERS
        // ==========================================
        bind(EconomyListener.class).asEagerSingleton();
        bind(TradeListener.class).asEagerSingleton();
        bind(BazaarChatListener.class).asEagerSingleton();

        // ==========================================
        // ⌨️ COMANDOS
        // ==========================================
        bind(ComandoEco.class).asEagerSingleton();
        bind(ComandoBazar.class).asEagerSingleton();
        bind(ComandoTrade.class).asEagerSingleton();
        bind(ComandoMercadoNegro.class).asEagerSingleton();
    }

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    /**
     * Le decimos a Guice explícitamente de dónde sacar el ItemManager
     * para que NO intente hacer "new NexoItems()".
     */
    @Provides
    @Singleton
    public ItemManager proveerItemManager() {
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el método estandarizado getInjector()
        NexoItems itemsPlugin = (NexoItems) Bukkit.getPluginManager().getPlugin("NexoItems");

        if (itemsPlugin != null && itemsPlugin.getInjector() != null) {
            return itemsPlugin.getInjector().getInstance(ItemManager.class);
        }

        return null;
    }
}
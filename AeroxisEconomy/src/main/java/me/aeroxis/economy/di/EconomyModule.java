package me.aeroxis.economy.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.economy.AeroxisEconomy;
import me.aeroxis.economy.commands.ComandoBazar;
import me.aeroxis.economy.commands.ComandoEco;
import me.aeroxis.economy.commands.ComandoMercadoNegro;
import me.aeroxis.economy.commands.ComandoTrade;
import org.bukkit.Bukkit;

import me.aeroxis.economy.bazar.BazaarChatListener;
import me.aeroxis.economy.bazar.BazaarManager;
import me.aeroxis.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.commands.*;
import me.aeroxis.economy.config.ConfigManager;
import me.aeroxis.economy.core.EconomyManager;
import me.aeroxis.economy.listeners.EconomyListener;
import me.aeroxis.economy.listeners.TradeListener;
import me.aeroxis.economy.trade.TradeManager;

// 🌟 FIX CRÍTICO: Importamos las clases de AeroxisItems
import me.aeroxis.items.AeroxisItems;
import me.aeroxis.items.managers.ItemManager;

/**
 * 💰 AeroxisEconomy - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) para prevenir Lag Spikes en Gameplay.
 */
public class EconomyModule extends AbstractModule {

    private final AeroxisEconomy plugin;

    public EconomyModule(AeroxisEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // ==========================================
        // ⚙️ CORE
        // ==========================================
        bind(AeroxisEconomy.class).toInstance(plugin);
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
     * para que NO intente hacer "new AeroxisItems()".
     */
    @Provides
    @Singleton
    public ItemManager proveerItemManager() {
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el método estandarizado getInjector()
        AeroxisItems itemsPlugin = (AeroxisItems) Bukkit.getPluginManager().getPlugin("AeroxisItems");

        if (itemsPlugin != null && itemsPlugin.getInjector() != null) {
            return itemsPlugin.getInjector().getInstance(ItemManager.class);
        }

        return null;
    }
}
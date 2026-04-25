package me.nexo.clans.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.clans.NexoClans;
import me.nexo.clans.commands.ComandoChatClan;
import me.nexo.clans.commands.ComandoClan;
import me.nexo.clans.config.ConfigManager;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.listeners.ClanDamageListener;

// 🌟 IMPORTACIONES DEL PUENTE HORIZONTAL
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;

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

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    @Provides
    @Singleton
    public EconomyManager proveerEconomyManager() {
        // Obtenemos la instancia real cargada por Bukkit
        NexoEconomy ecoPlugin = JavaPlugin.getPlugin(NexoEconomy.class);
        // Le sacamos el Mánager a su inyector para evitar el "Plugin already initialized!"
        return ecoPlugin.getChildInjector().getInstance(EconomyManager.class);
    }
}
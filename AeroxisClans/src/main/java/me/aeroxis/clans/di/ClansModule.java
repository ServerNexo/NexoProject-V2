package me.aeroxis.clans.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.clans.AeroxisClans;
import me.aeroxis.economy.AeroxisEconomy;
import org.bukkit.Bukkit; // 🌟 AÑADIDO: Importación segura de Bukkit

import me.aeroxis.clans.commands.ComandoChatClan;
import me.aeroxis.clans.commands.ComandoClan;
import me.aeroxis.clans.config.ConfigManager;
import me.aeroxis.clans.core.ClanManager;
import me.aeroxis.clans.listeners.ClanConnectionListener;
import me.aeroxis.clans.listeners.ClanDamageListener;

// 🌟 IMPORTACIONES DEL PUENTE HORIZONTAL
import me.aeroxis.economy.core.EconomyManager;

/**
 * 👥 AeroxisClans - Módulo de Inyección de Dependencias (Guice)
 * Rendimiento: Carga en memoria instantánea (Eager Singleton) para prevenir tirones de Lag.
 */
public class ClansModule extends AbstractModule {

    private final AeroxisClans plugin;

    public ClansModule(AeroxisClans plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Core y Configuración
        bind(AeroxisClans.class).toInstance(plugin);

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
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el método estandarizado getInjector()
        AeroxisEconomy ecoPlugin = (AeroxisEconomy) Bukkit.getPluginManager().getPlugin("AeroxisEconomy");
        if (ecoPlugin != null && ecoPlugin.getInjector() != null) {
            return ecoPlugin.getInjector().getInstance(EconomyManager.class);
        }
        return null;
    }
}
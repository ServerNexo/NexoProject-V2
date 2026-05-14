package me.aeroxis.protections.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.clans.AeroxisClans;
import me.aeroxis.protections.AeroxisProtections;
import org.bukkit.Bukkit; // 🌟 AÑADIDO: Importación segura de Bukkit

import me.aeroxis.protections.ProtectionsBootstrap;
import me.aeroxis.protections.commands.ComandoProteccion;
import me.aeroxis.protections.config.ConfigManager;
import me.aeroxis.protections.listeners.EnvironmentListener;
import me.aeroxis.protections.listeners.ProtectionListener;
import me.aeroxis.protections.managers.ClaimManager;
import me.aeroxis.protections.managers.LimitManager;
import me.aeroxis.protections.managers.UpkeepManager;

// 🌟 IMPORTACIONES DEL PUENTE HORIZONTAL
import me.aeroxis.clans.core.ClanManager;

/**
 * 💉 AeroxisProtections - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga en memoria instantánea (Eager Singleton) para prevenir tirones de Lag.
 * Nota: No es necesario enlazar AeroxisCore ni sus managers aquí, ya los hereda del Inyector Padre.
 */
public class ProtectionsModule extends AbstractModule {

    private final AeroxisProtections plugin;

    public ProtectionsModule(AeroxisProtections plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia del plugin
        bind(AeroxisProtections.class).toInstance(plugin);

        // ==========================================
        // 🚀 ORQUESTADOR Y CONFIGURACIÓN
        // ==========================================
        bind(ProtectionsBootstrap.class).asEagerSingleton();
        bind(ConfigManager.class).asEagerSingleton();

        // ==========================================
        // 🧠 CEREBROS (MANAGERS)
        // ==========================================
        bind(ClaimManager.class).asEagerSingleton();
        bind(LimitManager.class).asEagerSingleton();
        bind(UpkeepManager.class).asEagerSingleton();

        // ==========================================
        // 🎧 EVENTOS (LISTENERS)
        // ==========================================
        bind(ProtectionListener.class).asEagerSingleton();
        bind(EnvironmentListener.class).asEagerSingleton();

        // ==========================================
        // ⌨️ COMANDOS NATIVOS
        // ==========================================
        bind(ComandoProteccion.class).asEagerSingleton();
    }

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    /**
     * Le decimos a Guice explícitamente de dónde sacar el ClanManager
     * para que NO intente hacer "new AeroxisClans()".
     */
    @Provides
    @Singleton
    public ClanManager proveerClanManager() {
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el método estandarizado getInjector()
        AeroxisClans clansPlugin = (AeroxisClans) Bukkit.getPluginManager().getPlugin("AeroxisClans");

        if (clansPlugin != null && clansPlugin.getInjector() != null) {
            return clansPlugin.getInjector().getInstance(ClanManager.class);
        }

        return null;
    }
}
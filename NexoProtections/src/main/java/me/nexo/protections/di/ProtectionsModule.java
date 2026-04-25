package me.nexo.protections.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.protections.NexoProtections;
import me.nexo.protections.ProtectionsBootstrap;
import me.nexo.protections.commands.ComandoProteccion;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.listeners.EnvironmentListener;
import me.nexo.protections.listeners.ProtectionListener;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.protections.managers.LimitManager;
import me.nexo.protections.managers.UpkeepManager;

// 🌟 IMPORTACIONES DEL PUENTE HORIZONTAL
import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanManager;

/**
 * 💉 NexoProtections - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga en memoria instantánea (Eager Singleton) para prevenir tirones de Lag.
 * Nota: No es necesario enlazar NexoCore ni sus managers aquí, ya los hereda del Inyector Padre.
 */
public class ProtectionsModule extends AbstractModule {

    private final NexoProtections plugin;

    public ProtectionsModule(NexoProtections plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia del plugin
        bind(NexoProtections.class).toInstance(plugin);

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
     * para que NO intente hacer "new NexoClans()".
     */
    @Provides
    @Singleton
    public ClanManager proveerClanManager() {
        // 1. Buscamos la instancia oficial que PaperMC ya cargó
        NexoClans clansPlugin = JavaPlugin.getPlugin(NexoClans.class);

        // 2. Extraemos su manager del inyector hijo para compartir la misma memoria
        return clansPlugin.getChildInjector().getInstance(ClanManager.class);
    }
}
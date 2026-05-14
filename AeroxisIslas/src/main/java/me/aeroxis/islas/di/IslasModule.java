package me.aeroxis.islas.di;

import com.google.inject.AbstractModule;
import me.aeroxis.islas.AeroxisIslas;
import me.aeroxis.islas.commands.ComandoIsla;
import me.aeroxis.islas.data.IslandDatabase;
import me.aeroxis.islas.instances.IslandSlimeManager; // 🌟 NUESTRO MOTOR DE MUNDOS ASP
import me.aeroxis.islas.listeners.IslandListener;
import me.aeroxis.islas.listeners.IslandMechanicsListener; // 🌟 EL MOTOR DE MEJORAS FÍSICAS
import me.aeroxis.islas.listeners.IslandProgressionListener;
import me.aeroxis.islas.listeners.IslandSecurityListener;
import me.aeroxis.islas.managers.IslandManager;

/**
 * 🌴 AeroxisIslas - Módulo de Inyección de Dependencias
 * Arquitectura: Carga Inmediata (Eager) y Motor AdvancedSlimePaper.
 */
public class IslasModule extends AbstractModule {

    private final AeroxisIslas plugin;

    public IslasModule(AeroxisIslas plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // ==========================================
        // ⚙️ CORE
        // ==========================================
        bind(AeroxisIslas.class).toInstance(plugin);

        // ==========================================
        // 💾 DATOS Y PERSISTENCIA
        // ==========================================
        bind(IslandDatabase.class).asEagerSingleton();

        // ==========================================
        // 🧠 CEREBROS Y MOTORES (ASP)
        // ==========================================
        // Obligamos a Guice a instanciar el motor de mundos efímeros al instante
        bind(IslandSlimeManager.class).asEagerSingleton();
        bind(IslandManager.class).asEagerSingleton();

        // ==========================================
        // 🛡️ SEGURIDAD Y LISTENERS
        // ==========================================
        bind(IslandListener.class).asEagerSingleton();
        bind(IslandProgressionListener.class).asEagerSingleton();
        bind(IslandSecurityListener.class).asEagerSingleton();
        bind(IslandMechanicsListener.class).asEagerSingleton(); // 🌟 INYECTADO AQUÍ

        // ==========================================
        // ⌨️ COMANDOS
        // ==========================================
        bind(ComandoIsla.class).asEagerSingleton();
    }
}
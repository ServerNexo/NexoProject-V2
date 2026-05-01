package me.nexo.islas.di;

import com.google.inject.AbstractModule;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.commands.ComandoIsla;
import me.nexo.islas.data.IslandDatabase;
import me.nexo.islas.instances.IslandSlimeManager; // 🌟 NUESTRO MOTOR DE MUNDOS ASP
import me.nexo.islas.listeners.IslandListener;
import me.nexo.islas.listeners.IslandMechanicsListener; // 🌟 EL MOTOR DE MEJORAS FÍSICAS
import me.nexo.islas.listeners.IslandProgressionListener;
import me.nexo.islas.listeners.IslandSecurityListener;
import me.nexo.islas.managers.IslandManager;

/**
 * 🌴 NexoIslas - Módulo de Inyección de Dependencias
 * Arquitectura: Carga Inmediata (Eager) y Motor AdvancedSlimePaper.
 */
public class IslasModule extends AbstractModule {

    private final NexoIslas plugin;

    public IslasModule(NexoIslas plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // ==========================================
        // ⚙️ CORE
        // ==========================================
        bind(NexoIslas.class).toInstance(plugin);

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
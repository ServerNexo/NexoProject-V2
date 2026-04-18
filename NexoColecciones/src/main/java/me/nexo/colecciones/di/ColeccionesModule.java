package me.nexo.colecciones.di;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.FlushTask;
import me.nexo.colecciones.commands.ComandoColecciones;
import me.nexo.colecciones.commands.ComandoSlayer;
import me.nexo.colecciones.config.ConfigManager;
import me.nexo.colecciones.slayers.SlayerListener;
import me.nexo.colecciones.slayers.SlayerManager;

/**
 * 📚 NexoColecciones - Módulo de Inyección de Dependencias (Guice)
 */
public class ColeccionesModule extends AbstractModule {

    private final NexoColecciones plugin;

    public ColeccionesModule(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Core
        bind(NexoColecciones.class).toInstance(plugin);
        bind(ConfigManager.class).in(Scopes.SINGLETON);

        // 🌟 AÑADIDO: Configuración interna de las colecciones
        bind(ColeccionesConfig.class).in(Scopes.SINGLETON);

        // Cerebros (Managers)
        bind(CollectionManager.class).in(Scopes.SINGLETON);
        bind(SlayerManager.class).in(Scopes.SINGLETON);

        // Eventos y Tareas
        bind(ColeccionesListener.class).in(Scopes.SINGLETON);
        bind(SlayerListener.class).in(Scopes.SINGLETON);
        bind(FlushTask.class).in(Scopes.SINGLETON);

        // Comandos
        bind(ComandoColecciones.class).in(Scopes.SINGLETON);
        bind(ComandoSlayer.class).in(Scopes.SINGLETON);
    }
}
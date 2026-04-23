package me.nexo.war.di;

import com.google.inject.AbstractModule;
import me.nexo.war.NexoWar;
import me.nexo.war.managers.WarManager;
import me.nexo.war.config.ConfigManager;

/**
 * 💉 NexoWar - Módulo de Inyección de Dependencias (Arquitectura Enterprise)
 * Configura los enlaces específicos del submódulo de guerra.
 * Nota: UserManager, DatabaseManager y NexoCore se heredan automáticamente del CoreInjector.
 */
public class WarModule extends AbstractModule {

    private final NexoWar plugin;

    public WarModule(NexoWar plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // 🛡️ Vinculamos la instancia actual del plugin de guerra
        bind(NexoWar.class).toInstance(plugin);

        // ⚔️ REGISTRO DE COMPONENTES TÁCTICOS
        // Al usar 'asEagerSingleton', Guice los instancia al arrancar el plugin
        // evitando retrasos (lag) cuando un jugador use un comando por primera vez.
        bind(WarManager.class).asEagerSingleton();
        bind(ConfigManager.class).asEagerSingleton();
    }
}
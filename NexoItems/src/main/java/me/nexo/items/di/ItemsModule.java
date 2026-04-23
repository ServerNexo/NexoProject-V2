package me.nexo.items.di;

import com.google.inject.AbstractModule;
import me.nexo.core.NexoCore;
import me.nexo.items.NexoItems;

/**
 * 💉 NexoItems - Módulo de Inyección de Dependencias (Arquitectura Enterprise Java 21)
 * Rendimiento: Inyección Pura y Erradicación Total de Service Locators Estáticos.
 */
public class ItemsModule extends AbstractModule {

    private final NexoItems plugin;
    private final NexoCore corePlugin;

    // 💉 PILAR 1: Exigimos las instancias base desde el Bootstrap
    public ItemsModule(NexoItems plugin, NexoCore corePlugin) {
        this.plugin = plugin;
        this.corePlugin = corePlugin;
    }

    @Override
    protected void configure() {
        // Enlazamos las instancias principales de los plugins
        bind(NexoItems.class).toInstance(plugin);
        bind(NexoCore.class).toInstance(corePlugin);

        /* * 💡 NOTA DEL ARQUITECTO:
         * Si no estás usando un "Child Injector" que herede directamente de NexoCore,
         * este es el lugar exacto para enlazar los Managers del Core que purificamos en otras clases.
         * * Ejemplo:
         * bind(UserManager.class).toInstance(corePlugin.getUserManager());
         * bind(DatabaseManager.class).toInstance(corePlugin.getDatabaseManager());
         * bind(CrossplayUtils.class).toInstance(corePlugin.getCrossplayUtils());
         */
    }
}
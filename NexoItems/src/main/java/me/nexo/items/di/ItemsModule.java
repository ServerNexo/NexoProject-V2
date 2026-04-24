package me.nexo.items.di;

import com.google.inject.AbstractModule;
import me.nexo.items.NexoItems;

/**
 * 💉 NexoItems - Módulo de Inyección de Dependencias (Arquitectura Enterprise Java 21)
 * Rendimiento: Inyección Pura, Herencia de Child Injector y Cero Service Locators.
 */
public class ItemsModule extends AbstractModule {

    private final NexoItems plugin;

    // 🌟 FIX: Solo exigimos la instancia de este plugin.
    // NexoCore y sus herramientas se heredan automáticamente del Inyector Padre.
    public ItemsModule(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos SOLAMENTE la instancia principal de ESTE plugin
        bind(NexoItems.class).toInstance(plugin);

        /* * 💡 NOTA DEL ARQUITECTO:
         * Al usar 'createChildInjector' en la clase principal, no necesitas
         * bindear cosas como UserManager o CrossplayUtils aquí.
         * ¡Guice ya sabe dónde están gracias al Core!
         */
    }
}
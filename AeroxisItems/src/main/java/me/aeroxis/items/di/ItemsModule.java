package me.aeroxis.items.di;

import com.google.inject.AbstractModule;
import me.aeroxis.items.AeroxisItems;
import me.aeroxis.items.mecanicas.BossLootListener; // 🌟 NUEVO IMPORT

/**
 * 💉 AeroxisItems - Módulo de Inyección de Dependencias (Arquitectura Enterprise Java 21)
 * Rendimiento: Inyección Pura, Herencia de Child Injector y Cero Service Locators.
 */
public class ItemsModule extends AbstractModule {

    private final AeroxisItems plugin;

    // 🌟 FIX: Solo exigimos la instancia de este plugin.
    // AeroxisCore y sus herramientas se heredan automáticamente del Inyector Padre.
    public ItemsModule(AeroxisItems plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos SOLAMENTE la instancia principal de ESTE plugin
        bind(AeroxisItems.class).toInstance(plugin);

        // ==========================================
        // ⚔️ MECÁNICAS Y SISTEMAS DE BOTÍN
        // ==========================================
        bind(BossLootListener.class).asEagerSingleton(); // 🌟 INYECTADO Y PREPARADO

        /* * 💡 NOTA DEL ARQUITECTO:
         * Al usar 'createChildInjector' en la clase principal, no necesitas
         * bindear cosas como UserManager o CrossplayUtils aquí.
         * ¡Guice ya sabe dónde están gracias al Core!
         */
    }
}
package me.nexo.items.di;

import com.google.inject.AbstractModule;
import me.nexo.core.NexoCore;
import me.nexo.items.NexoItems;

/**
 * 💉 NexoItems - Módulo de Inyección de Dependencias
 */
public class ItemsModule extends AbstractModule {

    private final NexoItems plugin;

    public ItemsModule(NexoItems plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoItems.class).toInstance(plugin);

        // Conectamos con el motor principal
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        bind(NexoCore.class).toInstance(core);
    }
}
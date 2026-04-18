package me.nexo.protections.di;

import com.google.inject.AbstractModule;
import me.nexo.core.NexoCore;
import me.nexo.protections.NexoProtections;

/**
 * 💉 NexoProtections - Módulo de Inyección de Dependencias
 */
public class ProtectionsModule extends AbstractModule {

    private final NexoProtections plugin;

    public ProtectionsModule(NexoProtections plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoProtections.class).toInstance(plugin);

        // Conectamos con el motor principal
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        bind(NexoCore.class).toInstance(core);
    }
}
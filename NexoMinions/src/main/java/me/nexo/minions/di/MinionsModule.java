package me.nexo.minions.di;

import com.google.inject.AbstractModule;
import me.nexo.core.NexoCore;
import me.nexo.minions.NexoMinions;

/**
 * 💉 NexoMinions - Módulo de Inyección de Dependencias
 */
public class MinionsModule extends AbstractModule {

    private final NexoMinions plugin;

    public MinionsModule(NexoMinions plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoMinions.class).toInstance(plugin);

        // Conectamos con el motor principal
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        bind(NexoCore.class).toInstance(core);
    }
}
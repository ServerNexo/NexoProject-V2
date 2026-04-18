package me.nexo.mechanics.di;

import com.google.inject.AbstractModule;
import me.nexo.core.NexoCore;
import me.nexo.mechanics.NexoMechanics;

/**
 * 💉 NexoMechanics - Módulo de Inyección de Dependencias
 */
public class MechanicsModule extends AbstractModule {

    private final NexoMechanics plugin;

    public MechanicsModule(NexoMechanics plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoMechanics.class).toInstance(plugin);

        // Conectamos con el motor principal
        NexoCore core = NexoCore.getPlugin(NexoCore.class);
        bind(NexoCore.class).toInstance(core);
    }
}
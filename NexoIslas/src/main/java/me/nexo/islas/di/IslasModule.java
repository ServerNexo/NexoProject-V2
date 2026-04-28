package me.nexo.islas.di;

import com.google.inject.AbstractModule;
import me.nexo.islas.NexoIslas;

public class IslasModule extends AbstractModule {

    private final NexoIslas plugin;

    public IslasModule(NexoIslas plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoIslas.class).toInstance(plugin);
        // Guice se encarga automáticamente del resto gracias a @Inject y @Singleton
    }
}
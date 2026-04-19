package me.nexo.pvp.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.user.UserRepository;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.api.PvPBootstrap;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.pasivas.PasivasManager;
import me.nexo.pvp.pvp.PvPManager;

public class PvPModule extends AbstractModule {

    private final NexoPvP plugin;

    public PvPModule(NexoPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoPvP.class).toInstance(plugin);

        // Singletons locales
        bind(ConfigManager.class).asEagerSingleton();
        bind(PvPManager.class).asEagerSingleton();
        bind(PasivasManager.class).asEagerSingleton();
        bind(PvPBootstrap.class).asEagerSingleton();
    }

    // 🌟 FIX: "Lazy Loading" para extraer el DAO purificado sin causar NullPointer
    @Provides
    @Singleton
    public UserRepository provideUserRepository() {
        return NexoCore.getPlugin(NexoCore.class).getUserRepository();
    }

    @Provides
    @Singleton
    public NexoCore provideNexoCore() {
        return NexoCore.getPlugin(NexoCore.class);
    }
}
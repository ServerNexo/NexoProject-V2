package me.nexo.crates.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.crates.NexoCrates;
import org.bukkit.Bukkit;

public class CratesModule extends AbstractModule {

    private final NexoCrates plugin;

    public CratesModule(NexoCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoCrates.class).toInstance(plugin);
    }

    @Provides
    @Singleton
    public NexoCore provideNexoCore() {
        return (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
    }

    @Provides
    @Singleton
    public DatabaseManager provideDatabaseManager(NexoCore core) {
        return core.getInjector().getInstance(DatabaseManager.class);
    }

    @Provides
    @Singleton
    public CrossplayUtils provideCrossplayUtils(NexoCore core) {
        return core.getInjector().getInstance(CrossplayUtils.class);
    }
}
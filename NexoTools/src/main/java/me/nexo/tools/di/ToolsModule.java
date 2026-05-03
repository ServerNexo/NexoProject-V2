package me.nexo.tools.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import me.nexo.tools.NexoTools;
import org.bukkit.Bukkit;

public class ToolsModule extends AbstractModule {

    private final NexoTools plugin;

    public ToolsModule(NexoTools plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoTools.class).toInstance(plugin);
    }

    @Provides
    @Singleton
    public NexoCore provideNexoCore() {
        return (NexoCore) Bukkit.getPluginManager().getPlugin("NexoCore");
    }

    @Provides
    @Singleton
    public CrossplayUtils provideCrossplayUtils(NexoCore core) {
        return core.getInjector().getInstance(CrossplayUtils.class);
    }

    @Provides
    @Singleton
    public DatabaseManager provideDatabaseManager(NexoCore core) {
        return core.getInjector().getInstance(DatabaseManager.class);
    }
}
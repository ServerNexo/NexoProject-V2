package me.aeroxis.tools.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.database.DatabaseManager;
import me.aeroxis.tools.AeroxisTools;
import org.bukkit.Bukkit;

public class ToolsModule extends AbstractModule {

    private final AeroxisTools plugin;

    public ToolsModule(AeroxisTools plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(AeroxisTools.class).toInstance(plugin);
    }

    @Provides
    @Singleton
    public AeroxisCore provideNexoCore() {
        return (AeroxisCore) Bukkit.getPluginManager().getPlugin("AeroxisCore");
    }

    @Provides
    @Singleton
    public CrossplayUtils provideCrossplayUtils(AeroxisCore core) {
        return core.getInjector().getInstance(CrossplayUtils.class);
    }

    @Provides
    @Singleton
    public DatabaseManager provideDatabaseManager(AeroxisCore core) {
        return core.getInjector().getInstance(DatabaseManager.class);
    }
}
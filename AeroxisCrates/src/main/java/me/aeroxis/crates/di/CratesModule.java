package me.aeroxis.crates.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.database.DatabaseManager;
import me.aeroxis.crates.AeroxisCrates;
import org.bukkit.Bukkit;

public class CratesModule extends AbstractModule {

    private final AeroxisCrates plugin;

    public CratesModule(AeroxisCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(AeroxisCrates.class).toInstance(plugin);
    }

    @Provides
    @Singleton
    public AeroxisCore provideAeroxisCore() { // 🌟 FIX: Cambiado de provideNexoCore a provideAeroxisCore
        return (AeroxisCore) Bukkit.getPluginManager().getPlugin("AeroxisCore");
    }

    @Provides
    @Singleton
    public DatabaseManager provideDatabaseManager(AeroxisCore core) {
        return core.getInjector().getInstance(DatabaseManager.class);
    }

    @Provides
    @Singleton
    public CrossplayUtils provideCrossplayUtils(AeroxisCore core) {
        return core.getInjector().getInstance(CrossplayUtils.class);
    }
}
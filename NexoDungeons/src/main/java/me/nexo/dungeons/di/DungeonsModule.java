package me.nexo.dungeons.di;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.bosses.BossFightManager;
import me.nexo.dungeons.bosses.LootDistributor;
import me.nexo.dungeons.commands.ComandoDungeon;
import me.nexo.dungeons.config.ConfigManager;
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.grid.DungeonGridManager;
import me.nexo.dungeons.listeners.DungeonListener;
import me.nexo.dungeons.listeners.DungeonSecurityListener;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.dungeons.matchmaking.QueueManager;
import me.nexo.dungeons.waves.WaveManager;

/**
 * 🏰 NexoDungeons - Módulo de Inyección de Dependencias (Guice)
 */
public class DungeonsModule extends AbstractModule {

    private final NexoDungeons plugin;

    public DungeonsModule(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Core
        bind(NexoDungeons.class).toInstance(plugin);
        bind(ConfigManager.class).in(Scopes.SINGLETON);

        // Cerebros (Managers y Motores)
        bind(DungeonGridManager.class).in(Scopes.SINGLETON);
        bind(PuzzleEngine.class).in(Scopes.SINGLETON);
        bind(BossFightManager.class).in(Scopes.SINGLETON);
        bind(LootDistributor.class).in(Scopes.SINGLETON);
        bind(QueueManager.class).in(Scopes.SINGLETON);
        bind(WaveManager.class).in(Scopes.SINGLETON);

        // Seguridad y Listeners
        bind(DungeonListener.class).in(Scopes.SINGLETON);
        bind(DungeonSecurityListener.class).in(Scopes.SINGLETON);
        bind(LootProtectionListener.class).in(Scopes.SINGLETON);

        // Comandos
        bind(ComandoDungeon.class).in(Scopes.SINGLETON);
    }
}
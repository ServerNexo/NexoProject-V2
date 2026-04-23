package me.nexo.dungeons.di;

import com.google.inject.AbstractModule;
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
 * 🏰 NexoDungeons - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) para prevenir Lag Spikes en Gameplay.
 */
public class DungeonsModule extends AbstractModule {

    private final NexoDungeons plugin;

    public DungeonsModule(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // ==========================================
        // ⚙️ CORE
        // ==========================================
        bind(NexoDungeons.class).toInstance(plugin);
        bind(ConfigManager.class).asEagerSingleton();

        // ==========================================
        // 🧠 CEREBROS (Managers y Motores)
        // ==========================================
        bind(DungeonGridManager.class).asEagerSingleton();
        bind(PuzzleEngine.class).asEagerSingleton();
        bind(BossFightManager.class).asEagerSingleton();
        bind(LootDistributor.class).asEagerSingleton();
        bind(QueueManager.class).asEagerSingleton();
        bind(WaveManager.class).asEagerSingleton();

        // ==========================================
        // 🛡️ SEGURIDAD Y LISTENERS
        // ==========================================
        bind(DungeonListener.class).asEagerSingleton();
        bind(DungeonSecurityListener.class).asEagerSingleton();
        bind(LootProtectionListener.class).asEagerSingleton();

        // ==========================================
        // ⌨️ COMANDOS
        // ==========================================
        bind(ComandoDungeon.class).asEagerSingleton();
    }
}
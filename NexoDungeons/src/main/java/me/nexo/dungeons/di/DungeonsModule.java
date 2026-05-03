package me.nexo.dungeons.di;

import com.google.inject.AbstractModule;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.bosses.BossFightManager;
import me.nexo.dungeons.bosses.LootDistributor;
import me.nexo.dungeons.commands.ComandoBotin;
import me.nexo.dungeons.commands.ComandoDungeon;
import me.nexo.dungeons.config.ConfigManager;
import me.nexo.dungeons.engine.AbyssLootEngine;
import me.nexo.dungeons.engine.AbyssScalingEngine;
import me.nexo.dungeons.engine.NexoDungeonFactory;
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.instances.DungeonSlimeManager;
import me.nexo.dungeons.listeners.AbyssDeathListener;
import me.nexo.dungeons.listeners.AbyssLootListener;
import me.nexo.dungeons.listeners.DungeonListener;
import me.nexo.dungeons.listeners.DungeonSecurityListener;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.dungeons.matchmaking.QueueManager;
import me.nexo.dungeons.mechanics.AbyssBackpackManager;
import me.nexo.dungeons.nemesis.NemesisHuntListener; // 🌟 NUEVO
import me.nexo.dungeons.nemesis.NemesisListener; // 🌟 NUEVO
import me.nexo.dungeons.nemesis.NemesisManager; // 🌟 NUEVO
import me.nexo.dungeons.waves.WaveManager;

import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;
import org.bukkit.Bukkit;

/**
 * 🏰 NexoDungeons - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) y Cross-Module Injection.
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

        // INYECCIÓN CROSS-PLUGIN
        NexoEconomy ecoPlugin = (NexoEconomy) Bukkit.getPluginManager().getPlugin("NexoEconomy");
        if (ecoPlugin != null) {
            bind(NexoEconomy.class).toInstance(ecoPlugin);
            bind(EconomyManager.class).toInstance(ecoPlugin.getChildInjector().getInstance(EconomyManager.class));
        } else {
            plugin.getLogger().severe("❌ FATAL: NexoEconomy no está cargado.");
        }

        // ==========================================
        // 🧠 CEREBROS (Managers y Motores AAA)
        // ==========================================
        bind(DungeonSlimeManager.class).asEagerSingleton();
        bind(NexoDungeonFactory.class).asEagerSingleton();
        bind(PuzzleEngine.class).asEagerSingleton();
        bind(BossFightManager.class).asEagerSingleton();
        bind(LootDistributor.class).asEagerSingleton();
        bind(QueueManager.class).asEagerSingleton();
        bind(WaveManager.class).asEagerSingleton();

        // 🌟 SISTEMAS DEL ABISMO
        bind(AbyssBackpackManager.class).asEagerSingleton();
        bind(AbyssScalingEngine.class).asEagerSingleton();
        bind(AbyssLootEngine.class).asEagerSingleton();

        // 🌟 SISTEMA NÉMESIS
        bind(NemesisManager.class).asEagerSingleton();

        // ==========================================
        // 🛡️ SEGURIDAD Y LISTENERS
        // ==========================================
        bind(DungeonListener.class).asEagerSingleton();
        bind(DungeonSecurityListener.class).asEagerSingleton();
        bind(LootProtectionListener.class).asEagerSingleton();

        // 🌟 LISTENERS DEL ABISMO Y NÉMESIS
        bind(AbyssDeathListener.class).asEagerSingleton();
        bind(AbyssLootListener.class).asEagerSingleton();
        bind(NemesisListener.class).asEagerSingleton(); // El que crea al Némesis
        bind(NemesisHuntListener.class).asEagerSingleton(); // El que suelta el botín

        // ==========================================
        // ⌨️ COMANDOS
        // ==========================================
        bind(ComandoDungeon.class).asEagerSingleton();
        bind(ComandoBotin.class).asEagerSingleton();
    }
}
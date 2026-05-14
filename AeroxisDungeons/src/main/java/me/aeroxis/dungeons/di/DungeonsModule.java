package me.aeroxis.dungeons.di;

import com.google.inject.AbstractModule;
import me.aeroxis.dungeons.AeroxisDungeons;
import me.aeroxis.dungeons.bosses.BossFightManager;
import me.aeroxis.dungeons.bosses.LootDistributor;
import me.aeroxis.dungeons.commands.ComandoBotin;
import me.aeroxis.dungeons.commands.ComandoDungeon;
import me.aeroxis.dungeons.config.ConfigManager;
import me.aeroxis.dungeons.engine.AbyssLootEngine;
import me.aeroxis.dungeons.engine.AbyssScalingEngine;
import me.aeroxis.dungeons.engine.AeroxisDungeonFactory;
import me.aeroxis.dungeons.engine.PuzzleEngine;
import me.aeroxis.dungeons.instances.DungeonSlimeManager;
import me.aeroxis.dungeons.listeners.AbyssDeathListener;
import me.aeroxis.dungeons.listeners.AbyssLootListener;
import me.aeroxis.dungeons.listeners.DungeonListener;
import me.aeroxis.dungeons.listeners.DungeonSecurityListener;
import me.aeroxis.dungeons.listeners.LootProtectionListener;
import me.aeroxis.dungeons.matchmaking.QueueManager;
import me.aeroxis.dungeons.mechanics.AbyssBackpackManager;
import me.aeroxis.dungeons.nemesis.NemesisHuntListener; // 🌟 NUEVO
import me.aeroxis.dungeons.nemesis.NemesisListener; // 🌟 NUEVO
import me.aeroxis.dungeons.nemesis.NemesisManager; // 🌟 NUEVO
import me.aeroxis.dungeons.waves.WaveManager;

import me.aeroxis.economy.AeroxisEconomy;
import me.aeroxis.economy.core.EconomyManager;
import org.bukkit.Bukkit;

/**
 * 🏰 AeroxisDungeons - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) y Cross-Module Injection.
 */
public class DungeonsModule extends AbstractModule {

    private final AeroxisDungeons plugin;

    public DungeonsModule(AeroxisDungeons plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // ==========================================
        // ⚙️ CORE
        // ==========================================
        bind(AeroxisDungeons.class).toInstance(plugin);
        bind(ConfigManager.class).asEagerSingleton();

        // INYECCIÓN CROSS-PLUGIN
        AeroxisEconomy ecoPlugin = (AeroxisEconomy) Bukkit.getPluginManager().getPlugin("AeroxisEconomy");
        if (ecoPlugin != null) {
            bind(AeroxisEconomy.class).toInstance(ecoPlugin);
            // 🌟 FIX: Usamos el método estandarizado getInjector() en lugar de getChildInjector()
            bind(EconomyManager.class).toInstance(ecoPlugin.getInjector().getInstance(EconomyManager.class));
        } else {
            plugin.getLogger().severe("❌ FATAL: AeroxisEconomy no está cargado.");
        }

        // ==========================================
        // 🧠 CEREBROS (Managers y Motores AAA)
        // ==========================================
        bind(DungeonSlimeManager.class).asEagerSingleton();
        bind(AeroxisDungeonFactory.class).asEagerSingleton();
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
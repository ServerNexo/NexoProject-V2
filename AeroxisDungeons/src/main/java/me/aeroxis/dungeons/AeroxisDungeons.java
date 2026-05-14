package me.aeroxis.dungeons;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.bosses.AeroxisBossRegistry;
import me.aeroxis.core.crossplay.CrossplayUtils; // 🌟 NUEVO IMPORT: Utilidad
import me.aeroxis.dungeons.mobs.AeroxisRevenantBoss;
import me.aeroxis.dungeons.commands.ComandoBotin;
import me.aeroxis.dungeons.commands.ComandoDungeon;
import me.aeroxis.dungeons.commands.ComandoRitualTest;
import me.aeroxis.dungeons.config.ConfigManager;
import me.aeroxis.dungeons.di.DungeonsModule;
import me.aeroxis.dungeons.engine.AbyssLootEngine;
import me.aeroxis.dungeons.engine.AbyssScalingEngine;
import me.aeroxis.dungeons.engine.PuzzleEngine;
import me.aeroxis.dungeons.listeners.AbyssDeathListener;
import me.aeroxis.dungeons.listeners.AbyssLootListener;
import me.aeroxis.dungeons.listeners.DungeonListener;
import me.aeroxis.dungeons.listeners.DungeonSecurityListener;
import me.aeroxis.dungeons.listeners.LootProtectionListener;
import me.aeroxis.dungeons.matchmaking.QueueManager;
import me.aeroxis.dungeons.nemesis.NemesisHuntListener;
import me.aeroxis.dungeons.nemesis.NemesisListener;
import me.aeroxis.dungeons.waves.WaveManager;
import org.bukkit.plugin.java.JavaPlugin;

// 🌟 IMPORTACIÓN DE LAMP
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏰 AeroxisDungeons - Main Plugin Class (Arquitectura Enterprise)
 * Rendimiento: Child Injector, Lamp Command Handler y Cero Estáticos.
 */
public class AeroxisDungeons extends JavaPlugin {

    private Injector childInjector;

    private ConfigManager configManager;
    private WaveManager waveManager;
    private QueueManager queueManager;
    private PuzzleEngine puzzleEngine;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏰 Iniciando AeroxisDungeons (Generador de Instancias Seguro)...");

        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: Falta AeroxisCore. Las puertas de la mazmorra permanecerán cerradas.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 INICIALIZACIÓN DE GUICE
        this.childInjector = corePlugin.getInjector().createChildInjector(new DungeonsModule(this));

        // 🌟 OBTENEMOS LAS INSTANCIAS DESDE GUICE
        this.configManager = childInjector.getInstance(ConfigManager.class);
        this.waveManager = childInjector.getInstance(WaveManager.class);
        this.queueManager = childInjector.getInstance(QueueManager.class);
        this.puzzleEngine = childInjector.getInstance(PuzzleEngine.class);

        // 🌟 REGISTRO DE JEFES EN EL MOTOR DEL CORE
        var bossRegistry = childInjector.getInstance(AeroxisBossRegistry.class);
        var crossplayUtils = childInjector.getInstance(CrossplayUtils.class);

        bossRegistry.registerBoss("EL_RENACIDO", (callerPlugin, loc) -> {
            return new AeroxisRevenantBoss(callerPlugin, crossplayUtils);
        });

        // 🌟 REGISTRO DE EVENTOS INYECTADOS
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(DungeonListener.class), this);
        pm.registerEvents(childInjector.getInstance(DungeonSecurityListener.class), this);
        pm.registerEvents(childInjector.getInstance(LootProtectionListener.class), this);

        // 🌟 REGISTRO DE EVENTOS DEL ABISMO
        pm.registerEvents(childInjector.getInstance(AbyssDeathListener.class), this);
        pm.registerEvents(childInjector.getInstance(AbyssLootListener.class), this);
        pm.registerEvents(childInjector.getInstance(AbyssScalingEngine.class), this);
        pm.registerEvents(childInjector.getInstance(AbyssLootEngine.class), this);

        // 🌟 REGISTRO DE EVENTOS NÉMESIS
        pm.registerEvents(childInjector.getInstance(NemesisListener.class), this);
        pm.registerEvents(childInjector.getInstance(NemesisHuntListener.class), this);

        // =========================================================
        // 🌟 FIX: REGISTRO DE COMANDOS (REVXRSAL LAMP)
        // =========================================================
        BukkitCommandHandler commandHandler = BukkitCommandHandler.create(this);
        commandHandler.register(childInjector.getInstance(ComandoDungeon.class));
        commandHandler.register(childInjector.getInstance(ComandoBotin.class));
        commandHandler.register(childInjector.getInstance(ComandoRitualTest.class));

        getLogger().info("✅ AeroxisDungeons cargado exitosamente. Las puertas del abismo están abiertas.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏰 Apagando AeroxisDungeons...");
        getLogger().info("✅ AeroxisDungeons ha sido deshabilitado.");
    }

    // 🌟 FIX DEFINITIVO: Renombramos a getInjector() para estandarizar el ecosistema
    public Injector getInjector() {
        return childInjector;
    }

    @Deprecated
    public ConfigManager getConfigManager() { return configManager; }

    @Deprecated
    public WaveManager getWaveManager() { return waveManager; }

    @Deprecated
    public QueueManager getQueueManager() { return queueManager; }

    @Deprecated
    public PuzzleEngine getPuzzleEngine() { return puzzleEngine; }
}
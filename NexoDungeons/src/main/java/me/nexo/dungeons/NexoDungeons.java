package me.nexo.dungeons;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.dungeons.commands.ComandoBotin;
import me.nexo.dungeons.commands.ComandoDungeon;
import me.nexo.dungeons.config.ConfigManager;
import me.nexo.dungeons.di.DungeonsModule;
import me.nexo.dungeons.engine.AbyssLootEngine;
import me.nexo.dungeons.engine.AbyssScalingEngine;
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.listeners.AbyssDeathListener;
import me.nexo.dungeons.listeners.AbyssLootListener;
import me.nexo.dungeons.listeners.DungeonListener;
import me.nexo.dungeons.listeners.DungeonSecurityListener;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.dungeons.matchmaking.QueueManager;
import me.nexo.dungeons.nemesis.NemesisHuntListener;
import me.nexo.dungeons.nemesis.NemesisListener;
import me.nexo.dungeons.waves.WaveManager;
import org.bukkit.plugin.java.JavaPlugin;

// 🌟 IMPORTACIÓN DE LAMP
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏰 NexoDungeons - Main Plugin Class (Arquitectura Enterprise)
 * Rendimiento: Child Injector, Lamp Command Handler y Cero Estáticos.
 */
public class NexoDungeons extends JavaPlugin {

    private Injector childInjector;

    private ConfigManager configManager;
    private WaveManager waveManager;
    private QueueManager queueManager;
    private PuzzleEngine puzzleEngine;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏰 Iniciando NexoDungeons (Generador de Instancias Seguro)...");

        var corePlugin = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: Falta NexoCore. Las puertas de la mazmorra permanecerán cerradas.");
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

        getLogger().info("✅ NexoDungeons cargado exitosamente. Las puertas del abismo están abiertas.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏰 Apagando NexoDungeons...");
        getLogger().info("✅ NexoDungeons ha sido deshabilitado.");
    }

    public Injector getChildInjector() {
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
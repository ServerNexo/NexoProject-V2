package me.nexo.dungeons;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.user.NexoAPI;
import me.nexo.dungeons.commands.ComandoDungeon;
import me.nexo.dungeons.config.ConfigManager;
import me.nexo.dungeons.di.DungeonsModule;
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.listeners.DungeonListener;
import me.nexo.dungeons.listeners.DungeonSecurityListener;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.dungeons.matchmaking.QueueManager;
import me.nexo.dungeons.waves.WaveManager;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * 🏰 NexoDungeons - Main Plugin Class (Arquitectura NATIVA)
 */
public class NexoDungeons extends JavaPlugin {

    private Injector injector;
    private ConfigManager configManager;
    private WaveManager waveManager;
    private QueueManager queueManager;
    private PuzzleEngine puzzleEngine;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏰 Iniciando NexoDungeons (Generador de Instancias Seguro)...");

        // Verificación de seguridad: Dependemos del Core
        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ Error: Falta NexoCore. Las puertas de la mazmorra permanecerán cerradas.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 INICIALIZACIÓN DE GUICE (El motor Enterprise)
        this.injector = Guice.createInjector(new DungeonsModule(this));

        // 🌟 OBTENEMOS LAS INSTANCIAS DESDE GUICE
        this.configManager = injector.getInstance(ConfigManager.class);
        this.waveManager = injector.getInstance(WaveManager.class);
        this.queueManager = injector.getInstance(QueueManager.class);
        this.puzzleEngine = injector.getInstance(PuzzleEngine.class);

        // 🌟 REGISTRO EN LA API GLOBAL DE NEXO
        NexoAPI.getServices().register(WaveManager.class, this.waveManager);
        NexoAPI.getServices().register(QueueManager.class, this.queueManager);
        NexoAPI.getServices().register(PuzzleEngine.class, this.puzzleEngine);

        // 🌟 REGISTRO DE EVENTOS INYECTADOS
        getServer().getPluginManager().registerEvents(injector.getInstance(DungeonListener.class), this);
        getServer().getPluginManager().registerEvents(injector.getInstance(DungeonSecurityListener.class), this);
        getServer().getPluginManager().registerEvents(injector.getInstance(LootProtectionListener.class), this);

        // 🌟 FIX: INYECCIÓN DE COMANDOS NATIVOS POR REFLEXIÓN
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

            // 💉 Inyectamos el comando saltándonos la seguridad de Paper
            commandMap.register("nexodungeons", injector.getInstance(ComandoDungeon.class));

            getLogger().info("✅ Comando de Dungeons inyectado nativamente (Zero-Lag).");
        } catch (Exception e) {
            getLogger().severe("❌ Error inyectando comando de NexoDungeons: " + e.getMessage());
            e.printStackTrace();
        }

        getLogger().info("✅ NexoDungeons cargado exitosamente. Las puertas del abismo están abiertas.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏰 Apagando NexoDungeons...");

        // 🌟 DESREGISTRO LIMPIO DE LA API PARA EVITAR MEMORY LEAKS
        NexoAPI.getServices().unregister(WaveManager.class);
        NexoAPI.getServices().unregister(QueueManager.class);
        NexoAPI.getServices().unregister(PuzzleEngine.class);

        getLogger().info("✅ NexoDungeons ha sido deshabilitado.");
    }

    // 🌟 GETTERS
    // Mantenidos por si alguna clase Vanilla que aún no inyectamos necesita leer los managers
    public ConfigManager getConfigManager() { return configManager; }
    public WaveManager getWaveManager() { return waveManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public PuzzleEngine getPuzzleEngine() { return puzzleEngine; }
}
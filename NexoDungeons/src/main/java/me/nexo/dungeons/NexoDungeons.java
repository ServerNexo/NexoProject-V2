package me.nexo.dungeons;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.dungeons.commands.ComandoDungeon;
import me.nexo.dungeons.config.ConfigManager;
import me.nexo.dungeons.di.DungeonsModule;
import me.nexo.dungeons.engine.PuzzleEngine;
import me.nexo.dungeons.listeners.DungeonListener;
import me.nexo.dungeons.listeners.DungeonSecurityListener;
import me.nexo.dungeons.listeners.LootProtectionListener;
import me.nexo.dungeons.matchmaking.QueueManager;
import me.nexo.dungeons.waves.WaveManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 🏰 NexoDungeons - Main Plugin Class (Arquitectura Enterprise)
 * Rendimiento: Child Injector, Inyección Nativa en CommandMap y Cero Estáticos.
 */
public class NexoDungeons extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core y Economía)
    private Injector childInjector;
    
    private ConfigManager configManager;
    private WaveManager waveManager;
    private QueueManager queueManager;
    private PuzzleEngine puzzleEngine;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏰 Iniciando NexoDungeons (Generador de Instancias Seguro)...");

        // Verificación de seguridad y obtención del Core
        var corePlugin = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: Falta NexoCore. Las puertas de la mazmorra permanecerán cerradas.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 INICIALIZACIÓN DE GUICE: Creamos el inyector hijo
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

        // 🌟 REGISTRO NATIVO DE COMANDOS (PAPER 1.21.5 FIX)
        // Bypass del bloqueo de getCommand() inyectando directamente en el CommandMap.
        var commandMap = getServer().getCommandMap();
        commandMap.register("nexodungeons", childInjector.getInstance(ComandoDungeon.class));

        getLogger().info("✅ NexoDungeons cargado exitosamente. Las puertas del abismo están abiertas.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏰 Apagando NexoDungeons...");
        // Al no usar el Service Locator estático, el recolector de basura (GC) 
        // limpiará los managers inyectados automáticamente sin riesgo de memory leaks.
        getLogger().info("✅ NexoDungeons ha sido deshabilitado.");
    }

    public Injector getChildInjector() {
        return childInjector;
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS (PUENTE LEGACY)
    // Guice ya gestiona la caché O(1) internamente. Usar @Inject en constructores.
    // ==========================================
    
    @Deprecated
    public ConfigManager getConfigManager() { return configManager; }
    
    @Deprecated
    public WaveManager getWaveManager() { return waveManager; }
    
    @Deprecated
    public QueueManager getQueueManager() { return queueManager; }
    
    @Deprecated
    public PuzzleEngine getPuzzleEngine() { return puzzleEngine; }
}
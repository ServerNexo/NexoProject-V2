package me.nexo.minions;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.TiersConfig;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.di.MinionsModule;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 🤖 NexoMinions - Clase Principal (Arquitectura Enterprise)
 */
public class NexoMinions extends JavaPlugin {

    private Injector injector;
    private MinionsBootstrap bootstrap;

    // 🌟 FIX: Variables cacheadas para los Getters (Cero estrés al Injector)
    private MinionManager minionManager;
    private TiersConfig tiersConfig;
    private UpgradesConfig upgradesConfig;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🤖 Iniciando NexoMinions (Motor Enterprise)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 1. Inicializar Inyección
        this.injector = Guice.createInjector(new MinionsModule(this));

        // 🌟 2. Forzamos la carga y guardamos referencias O(1)
        this.configManager = injector.getInstance(ConfigManager.class);
        this.tiersConfig = injector.getInstance(TiersConfig.class);
        this.upgradesConfig = injector.getInstance(UpgradesConfig.class);
        this.minionManager = injector.getInstance(MinionManager.class);

        // 🚀 3. Arrancar Orquestador
        this.bootstrap = injector.getInstance(MinionsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡NexoMinions cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // ==========================================
    // 💡 GETTERS DE ALTO RENDIMIENTO
    // ==========================================
    public MinionManager getMinionManager() { return minionManager; }
    public TiersConfig getTiersConfig() { return tiersConfig; }
    public UpgradesConfig getUpgradesConfig() { return upgradesConfig; }
    public ConfigManager getConfigManager() { return configManager; }
}
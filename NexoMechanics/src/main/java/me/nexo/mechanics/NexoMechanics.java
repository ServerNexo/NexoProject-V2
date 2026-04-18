package me.nexo.mechanics;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.di.MechanicsModule;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoMechanics extends JavaPlugin {

    private Injector injector;
    private MechanicsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚙️ Iniciando NexoMechanics (Motor Enterprise)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 Inicializar Inyección
        this.injector = Guice.createInjector(new MechanicsModule(this));

        // 🚀 Arrancar Orquestador
        this.bootstrap = injector.getInstance(MechanicsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡NexoMechanics cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS
    // ==========================================
    public ConfigManager getConfigManager() {
        return injector.getInstance(ConfigManager.class);
    }
}
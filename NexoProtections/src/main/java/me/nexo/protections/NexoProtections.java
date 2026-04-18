package me.nexo.protections;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.di.ProtectionsModule;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoProtections extends JavaPlugin {

    private Injector injector;
    private ProtectionsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🛡️ Iniciando NexoProtections (Motor Enterprise)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 Inicializar Inyección
        this.injector = Guice.createInjector(new ProtectionsModule(this));

        // 🚀 Arrancar Orquestador
        this.bootstrap = injector.getInstance(ProtectionsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡NexoProtections cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // 🌟 Comando para recargar (usado desde ComandoProteccion.java)
    public void reloadSystem() {
        getLogger().info("🔄 Recargando NexoProtections...");

        ClaimManager claimManager = injector.getInstance(ClaimManager.class);
        claimManager.getAllStones().clear();
        claimManager.loadAllStonesAsync();

        // 💡 También recargamos los textos
        injector.getInstance(ConfigManager.class).reloadMessages();
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS
    // ==========================================
    public ConfigManager getConfigManager() {
        return injector.getInstance(ConfigManager.class);
    }

    public ClaimManager getClaimManager() {
        return injector.getInstance(ClaimManager.class);
    }
}
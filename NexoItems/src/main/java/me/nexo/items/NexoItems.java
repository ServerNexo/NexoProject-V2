package me.nexo.items;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.items.accesorios.AccesoriosManager;
import me.nexo.items.artefactos.ArtefactoManager;
import me.nexo.items.config.ConfigManager;
import me.nexo.items.di.ItemsModule;
import me.nexo.items.guardarropa.GuardarropaManager;
import me.nexo.items.managers.FileManager;
import me.nexo.items.mochilas.MochilaManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoItems extends JavaPlugin {

    private Injector injector;
    private ItemsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🎒 Iniciando NexoItems (Motor Enterprise)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 Inicializar Inyección
        this.injector = Guice.createInjector(new ItemsModule(this));

        // 🚀 Arrancar Orquestador
        this.bootstrap = injector.getInstance(ItemsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡NexoItems cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // ==========================================
    // 💡 GETTERS DE COMPATIBILIDAD
    // ==========================================
    public ConfigManager getConfigManager() { return injector.getInstance(ConfigManager.class); }
    public FileManager getFileManager() { return injector.getInstance(FileManager.class); }
    public AccesoriosManager getAccesoriosManager() { return injector.getInstance(AccesoriosManager.class); }
    public ArtefactoManager getArtefactoManager() { return injector.getInstance(ArtefactoManager.class); }
    public GuardarropaManager getGuardarropaManager() { return injector.getInstance(GuardarropaManager.class); }
    public MochilaManager getMochilaManager() { return injector.getInstance(MochilaManager.class); }
}
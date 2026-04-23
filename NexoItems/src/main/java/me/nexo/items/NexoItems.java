package me.nexo.items;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.items.accesorios.AccesoriosManager;
import me.nexo.items.artefactos.ArtefactoManager;
import me.nexo.items.config.ConfigManager;
import me.nexo.items.di.ItemsModule;
import me.nexo.items.guardarropa.GuardarropaManager;
import me.nexo.items.managers.FileManager;
import me.nexo.items.mochilas.MochilaManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 🎒 NexoItems - Main Plugin Class (Arquitectura Enterprise)
 * Rendimiento: Child Injector (Sinergia con NexoCore) y Orquestador de Servicios.
 */
public class NexoItems extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales del Core
    private Injector childInjector;
    private ItemsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🎒 Iniciando NexoItems (Motor Enterprise)...");

        // 🛡️ Verificación estricta y casteo a la dependencia Core
        var corePlugin = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: NexoCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 INICIALIZACIÓN DE GUICE: Creamos el inyector hijo
        this.childInjector = corePlugin.getInjector().createChildInjector(new ItemsModule(this));

        // 🚀 Arrancar Orquestador
        this.bootstrap = childInjector.getInstance(ItemsBootstrap.class);
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

    public Injector getChildInjector() {
        return childInjector;
    }

    // ==========================================
    // 💡 GETTERS DE COMPATIBILIDAD (PUENTE LEGACY)
    // Guice ya gestiona la caché O(1) internamente. Usar @Inject en constructores.
    // ==========================================
    
    @Deprecated
    public ConfigManager getConfigManager() { return childInjector.getInstance(ConfigManager.class); }
    @Deprecated
    public FileManager getFileManager() { return childInjector.getInstance(FileManager.class); }
    @Deprecated
    public AccesoriosManager getAccesoriosManager() { return childInjector.getInstance(AccesoriosManager.class); }
    @Deprecated
    public ArtefactoManager getArtefactoManager() { return childInjector.getInstance(ArtefactoManager.class); }
    @Deprecated
    public GuardarropaManager getGuardarropaManager() { return childInjector.getInstance(GuardarropaManager.class); }
    @Deprecated
    public MochilaManager getMochilaManager() { return childInjector.getInstance(MochilaManager.class); }
}
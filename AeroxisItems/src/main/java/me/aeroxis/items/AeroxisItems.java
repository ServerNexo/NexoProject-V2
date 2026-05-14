package me.aeroxis.items;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.items.accesorios.AccesoriosManager;
import me.aeroxis.items.artefactos.ArtefactoManager;
import me.aeroxis.items.config.ConfigManager;
import me.aeroxis.items.di.ItemsModule;
import me.aeroxis.items.guardarropa.GuardarropaManager;
import me.aeroxis.items.managers.FileManager;
import me.aeroxis.items.mochilas.MochilaManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 🎒 AeroxisItems - Main Plugin Class (Arquitectura Enterprise)
 * Rendimiento: Child Injector (Sinergia con AeroxisCore) y Orquestador de Servicios.
 */
public class AeroxisItems extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales del Core
    private Injector childInjector;
    private ItemsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🎒 Iniciando AeroxisItems (Motor Enterprise)...");

        // 🛡️ Verificación estricta y casteo a la dependencia Core
        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: AeroxisCore no detectado. Apagando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 INICIALIZACIÓN DE GUICE: Creamos el inyector hijo
        // 🌟 FIX: Ahora esto compilará en verde, porque ItemsModule solo pide 1 parámetro.
        this.childInjector = corePlugin.getInjector().createChildInjector(new ItemsModule(this));

        // 🚀 Arrancar Orquestador
        this.bootstrap = childInjector.getInstance(ItemsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡AeroxisItems cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // 🌟 FIX DEFINITIVO: Renombramos a getInjector() para estandarizar el ecosistema y evitar crasheos en NexoFactories
    public Injector getInjector() {
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
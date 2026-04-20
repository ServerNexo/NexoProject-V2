package me.nexo.items;

import com.google.inject.Guice;
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

public class NexoItems extends JavaPlugin {

    private Injector injector;
    private ItemsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🎒 Pre-iniciando NexoItems (Esperando enlace seguro con el Core)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando módulo de Ítems por seguridad...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 EL CANDADO: Esperamos a que NexoCore confirme que la BD y su Guice están listos
        NexoCore.getInstance().getCoreReadyFuture().thenRun(() -> {

            getLogger().info("🔓 Luz verde recibida de NexoCore. Arrancando motor de ítems custom...");

            // 💉 Inicializar Inyección de forma segura
            this.injector = Guice.createInjector(new ItemsModule(this));

            // 🚀 Arrancar Orquestador
            this.bootstrap = injector.getInstance(ItemsBootstrap.class);
            this.bootstrap.startServices();

            getLogger().info("✅ ¡NexoItems cargado y operativo!");
            getLogger().info("========================================");

        }).exceptionally(ex -> {
            getLogger().severe("❌ Error fatal esperando al Core en NexoItems: " + ex.getMessage());
            return null;
        });
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // ==========================================
    // 💡 GETTERS DE COMPATIBILIDAD (Protegidos)
    // ==========================================
    public ConfigManager getConfigManager() {
        if (injector == null) return null;
        return injector.getInstance(ConfigManager.class);
    }

    public FileManager getFileManager() {
        if (injector == null) return null;
        return injector.getInstance(FileManager.class);
    }

    public AccesoriosManager getAccesoriosManager() {
        if (injector == null) return null;
        return injector.getInstance(AccesoriosManager.class);
    }

    public ArtefactoManager getArtefactoManager() {
        if (injector == null) return null;
        return injector.getInstance(ArtefactoManager.class);
    }

    public GuardarropaManager getGuardarropaManager() {
        if (injector == null) return null;
        return injector.getInstance(GuardarropaManager.class);
    }

    public MochilaManager getMochilaManager() {
        if (injector == null) return null;
        return injector.getInstance(MochilaManager.class);
    }
}
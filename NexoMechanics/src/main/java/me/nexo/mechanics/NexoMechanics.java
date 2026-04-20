package me.nexo.mechanics;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.di.MechanicsModule;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoMechanics extends JavaPlugin {

    private Injector injector;
    private MechanicsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚙️ Pre-iniciando NexoMechanics (Esperando enlace seguro con el Core)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando módulo de Mecánicas por seguridad...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 EL CANDADO: Esperamos a que NexoCore confirme que la BD y su Guice están listos
        NexoCore.getInstance().getCoreReadyFuture().thenRun(() -> {

            getLogger().info("🔓 Luz verde recibida de NexoCore. Arrancando motor de mecánicas...");

            // 💉 Inicializar Inyección de forma segura
            this.injector = Guice.createInjector(new MechanicsModule(this));

            // 🚀 Arrancar Orquestador
            this.bootstrap = injector.getInstance(MechanicsBootstrap.class);
            this.bootstrap.startServices();

            getLogger().info("✅ ¡NexoMechanics cargado y operativo!");
            getLogger().info("========================================");

        }).exceptionally(ex -> {
            getLogger().severe("❌ Error fatal esperando al Core en NexoMechanics: " + ex.getMessage());
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
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS
    // ==========================================
    public ConfigManager getConfigManager() {
        if (injector == null) return null; // 🛡️ Protección anti-NPE
        return injector.getInstance(ConfigManager.class);
    }
}
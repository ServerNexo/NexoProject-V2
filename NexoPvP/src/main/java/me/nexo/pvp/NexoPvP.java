package me.nexo.pvp;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.pvp.api.PvPBootstrap;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.di.PvPModule;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ⚔️ NexoPvP - Núcleo de Combate (Arquitectura NATIVA + Anti-Race Conditions)
 */
public class NexoPvP extends JavaPlugin {

    private Injector injector;
    private PvPBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚔️ Pre-iniciando NexoPvP (Esperando enlace seguro con el Core)...");

        // 🛡️ Verificación estricta de la dependencia Core
        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ Error: Falta NexoCore. Apagando módulo de PvP por seguridad.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 EL CANDADO: Esperamos a que NexoCore confirme que la BD y su Guice están listos
        NexoCore.getInstance().getCoreReadyFuture().thenRun(() -> {

            getLogger().info("🔓 Luz verde recibida de NexoCore. Arrancando núcleo de combate...");

            // 💉 Iniciamos Guice y el Módulo PvP de forma segura
            this.injector = Guice.createInjector(new PvPModule(this));
            this.bootstrap = injector.getInstance(PvPBootstrap.class);

            // 🚀 Encendemos todos los servicios
            this.bootstrap.startServices();

            getLogger().info("✅ NexoPvP en línea.");
            getLogger().info("========================================");

        }).exceptionally(ex -> {
            getLogger().severe("❌ Error fatal esperando al Core en NexoPvP: " + ex.getMessage());
            return null;
        });
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.stopServices();
        }
    }

    // 🌉 PUENTE LEGACY (Para las clases que aún no purificamos)
    @Deprecated
    public ConfigManager getConfigManager() {
        if (injector == null) return null; // 🛡️ Protección extra por si se llama antes de tiempo
        return injector.getInstance(ConfigManager.class);
    }
}
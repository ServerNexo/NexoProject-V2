package me.nexo.war;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.war.config.ConfigManager;
import me.nexo.war.di.WarModule;
import me.nexo.war.managers.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NexoWar extends JavaPlugin {

    private Injector injector;
    private WarBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚔️ Pre-iniciando NexoWar (Esperando enlace seguro con el Core)...");

        // 🛡️ Verificación estricta de la dependencia Core
        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ Error: Falta NexoCore. Apagando módulo de Guerras por seguridad.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 EL CANDADO: Esperamos a que NexoCore confirme que la BD y su Guice están listos
        NexoCore.getInstance().getCoreReadyFuture().thenRun(() -> {

            getLogger().info("🔓 Luz verde recibida de NexoCore. Arrancando sistema de clanes y contratos...");

            // 💉 Inicializar el motor de Inyección (Guice) de forma segura
            this.injector = Guice.createInjector(new WarModule(this));

            // 🚀 Arrancar el Orquestador (Bootstrap)
            this.bootstrap = injector.getInstance(WarBootstrap.class);
            this.bootstrap.startServices();

        }).exceptionally(ex -> {
            getLogger().severe("❌ Error fatal esperando al Core en NexoWar: " + ex.getMessage());
            return null;
        });
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // 🌟 Mantenemos los getters extrayéndolos del Injector por si otras APIs los necesitan
    public WarManager getWarManager() {
        if (injector == null) return null;
        return injector.getInstance(WarManager.class);
    }

    public ConfigManager getConfigManager() {
        if (injector == null) return null;
        return injector.getInstance(ConfigManager.class);
    }
}
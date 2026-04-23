package me.nexo.pvp;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.pvp.api.PvPBootstrap;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.di.PvPModule;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ⚔️ NexoPvP - Núcleo de Combate (Arquitectura Enterprise)
 * Conectado orgánicamente al Inyector de NexoCore.
 */
public class NexoPvP extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    private PvPBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("⚔️ Sincronizando NexoPvP con el Core Engine...");

        // 💉 FUNDAMENTAL: Obtenemos el inyector principal de NexoCore.
        // Bukkit garantiza que NexoCore se carga primero gracias a paper-plugin.yml (depend: [NexoCore])
        Injector coreInjector = NexoCore.getInstance().getInjector();

        // 🧬 Creamos el Inyector Hijo. Ahora NexoPvP puede pedir @Inject UserManager sin fallar.
        this.childInjector = coreInjector.createChildInjector(new PvPModule(this));

        // 🚀 Encendemos el orquestador
        this.bootstrap = childInjector.getInstance(PvPBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ NexoPvP acoplado e iniciado con éxito.");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
        getLogger().info("⚔️ NexoPvP detenido de forma segura.");
    }

    // ==========================================================
    // 🌐 MÉTODOS DE API EXTERNA / PUENTE LEGACY
    // ==========================================================

    @Deprecated
    public ConfigManager getConfigManager() {
        return childInjector.getInstance(ConfigManager.class);
    }

    /**
     * Expone el inyector hijo por si submódulos internos muy complejos lo requirieran.
     */
    public Injector getChildInjector() {
        return childInjector;
    }
}
package me.aeroxis.pvp;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.pvp.api.PvPBootstrap;
import me.aeroxis.pvp.config.ConfigManager;
import me.aeroxis.pvp.di.PvPModule;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * ⚔️ AeroxisPvP - Núcleo de Combate (Arquitectura Enterprise)
 * Conectado orgánicamente al Inyector de AeroxisCore.
 */
public class AeroxisPvP extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    private PvPBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("⚔️ Sincronizando AeroxisPvP con el Core Engine...");

        // 🌟 FIX: Obtenemos el Core de forma segura mediante el PluginManager (Cero estáticos)
        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ FATAL: AeroxisCore no detectado. Apagando AeroxisPvP...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 FUNDAMENTAL: Obtenemos el inyector principal de AeroxisCore de la instancia real.
        Injector coreInjector = corePlugin.getInjector();

        // 🧬 Creamos el Inyector Hijo. Ahora AeroxisPvP puede pedir @Inject UserManager sin fallar.
        this.childInjector = coreInjector.createChildInjector(new PvPModule(this));

        // 🚀 Encendemos el orquestador
        this.bootstrap = childInjector.getInstance(PvPBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ AeroxisPvP acoplado e iniciado con éxito.");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
        getLogger().info("⚔️ AeroxisPvP detenido de forma segura.");
    }

    // ==========================================================
    // 🛡️ BYPASS NATIVO PAPER 1.21.5 (Evita crasheo de Lamp)
    // ==========================================================
    @Override
    public PluginCommand getCommand(@NotNull String name) {
        return null; // Forzamos a Lamp a usar el CommandMap directamente
    }

    // ==========================================================
    // 🌐 MÉTODOS DE API EXTERNA / PUENTE LEGACY
    // ==========================================================

    @Deprecated
    public ConfigManager getConfigManager() {
        return childInjector.getInstance(ConfigManager.class);
    }

    /**
     * 🌟 FIX DEFINITIVO: Renombramos a getInjector() para estandarizar el ecosistema
     */
    public Injector getInjector() {
        return childInjector;
    }
}
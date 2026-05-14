package me.aeroxis.war;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.war.config.ConfigManager;
import me.aeroxis.war.di.WarModule;
import me.aeroxis.war.managers.WarManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 🏛️ Nexo Network - AeroxisWar Sub-Module (Arquitectura Enterprise)
 * Motor del módulo de guerra conectado orgánicamente al Inyector del AeroxisCore.
 */
public class AeroxisWar extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar las dependencias del Core
    private Injector childInjector;
    private WarBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("⚔️ Sincronizando AeroxisWar con el Core Engine...");

        // 🌟 FIX: Obtenemos el Core de forma segura mediante el PluginManager (Cero estáticos)
        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ FATAL: AeroxisCore no detectado. Apagando AeroxisWar...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 FUNDAMENTAL: Obtenemos el inyector principal de AeroxisCore de la instancia real.
        Injector coreInjector = corePlugin.getInjector();

        // 🧬 Creamos el Inyector Hijo. Ahora AeroxisWar puede pedir @Inject UserManager sin fallar.
        this.childInjector = coreInjector.createChildInjector(new WarModule(this));

        // 🚀 Arrancar el Orquestador (Bootstrap)
        this.bootstrap = childInjector.getInstance(WarBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ AeroxisWar acoplado e iniciado con éxito.");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
        getLogger().info("⚔️ AeroxisWar detenido de forma segura.");
    }

    // ==========================================================
    // 🛡️ BYPASS NATIVO PAPER 1.21.5 (Evita crasheo de Lamp)
    // ==========================================================
    @Override
    public PluginCommand getCommand(@NotNull String name) {
        return null; // Forzamos a Lamp a usar el CommandMap directamente
    }

    // 🌟 FIX DEFINITIVO: Renombramos a getInjector() para estandarizar el ecosistema
    public Injector getInjector() {
        return childInjector;
    }

    // ==========================================================
    // 🌐 MÉTODOS DE API EXTERNA
    // Mantenemos los getters extrayéndolos del Injector por si
    // plugins de terceros (fuera de Guice) necesitan interactuar.
    // ==========================================================

    @Deprecated
    public WarManager getWarManager() {
        return childInjector.getInstance(WarManager.class);
    }

    @Deprecated
    public ConfigManager getConfigManager() {
        return childInjector.getInstance(ConfigManager.class);
    }
}
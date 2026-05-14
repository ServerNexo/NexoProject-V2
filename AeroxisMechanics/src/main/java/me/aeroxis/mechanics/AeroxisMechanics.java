package me.aeroxis.mechanics;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.mechanics.config.ConfigManager;
import me.aeroxis.mechanics.di.MechanicsModule;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * ⚙️ AeroxisMechanics - Clase Principal (Arquitectura Enterprise)
 * Rendimiento: Child Injector (Guice) heredado del Core y Orquestador de Servicios.
 */
public class AeroxisMechanics extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    private MechanicsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚙️ Iniciando AeroxisMechanics (Motor Enterprise)...");

        // 🌟 FIX: Obtenemos el Core de forma segura mediante el PluginManager (Cero estáticos)
        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ FATAL: AeroxisCore no detectado. Apagando el módulo de Mecánicas...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 1. Inicializar Inyección como Child Injector
        // Permite acceder a DatabaseManager y CrossplayUtils sin duplicar conexiones
        this.childInjector = corePlugin.getInjector().createChildInjector(new MechanicsModule(this));

        // 🚀 2. Arrancar Orquestador Inyectado
        this.bootstrap = childInjector.getInstance(MechanicsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡AeroxisMechanics cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
        getLogger().info("⚙️ AeroxisMechanics apagado de forma segura.");
    }

    // ==========================================================
    // 🛡️ BYPASS NATIVO PAPER 1.21.5 (Evita crasheo de Lamp)
    // ==========================================================
    @Override
    public PluginCommand getCommand(@NotNull String name) {
        return null; // Forzamos a Lamp a usar el CommandMap directamente
    }

    // 🌟 FIX DEFINITIVO: Renombramos a getInjector() para estandarizar todo el ecosistema
    public Injector getInjector() {
        return childInjector;
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS (PUENTE LEGACY)
    // Guice ya gestiona la caché O(1) internamente. Usar @Inject en constructores.
    // ==========================================

    @Deprecated
    public ConfigManager getConfigManager() {
        return childInjector.getInstance(ConfigManager.class);
    }
}
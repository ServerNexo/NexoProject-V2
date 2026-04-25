package me.nexo.mechanics;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.di.MechanicsModule;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * ⚙️ NexoMechanics - Clase Principal (Arquitectura Enterprise)
 * Rendimiento: Child Injector (Guice) heredado del Core y Orquestador de Servicios.
 */
public class NexoMechanics extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    private MechanicsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("⚙️ Iniciando NexoMechanics (Motor Enterprise)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ FATAL: NexoCore no detectado. Apagando el módulo de Mecánicas...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 1. Inicializar Inyección como Child Injector
        // Permite acceder a DatabaseManager y CrossplayUtils sin duplicar conexiones
        this.childInjector = NexoCore.getInstance().getInjector().createChildInjector(new MechanicsModule(this));

        // 🚀 2. Arrancar Orquestador Inyectado
        this.bootstrap = childInjector.getInstance(MechanicsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡NexoMechanics cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
        getLogger().info("⚙️ NexoMechanics apagado de forma segura.");
    }

    // ==========================================================
    // 🛡️ BYPASS NATIVO PAPER 1.21.5 (Evita crasheo de Lamp)
    // ==========================================================
    @Override
    public PluginCommand getCommand(@NotNull String name) {
        return null; // Forzamos a Lamp a usar el CommandMap directamente
    }

    public Injector getChildInjector() {
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
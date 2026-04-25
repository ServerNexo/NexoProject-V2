package me.nexo.minions;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.TiersConfig;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.di.MinionsModule;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 🤖 NexoMinions - Clase Principal (Arquitectura Enterprise)
 * Rendimiento: Child Injector (Guice) heredado del Core y Orquestador de Servicios.
 */
public class NexoMinions extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    private MinionsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🤖 Iniciando NexoMinions (Motor Enterprise)...");

        // 🌟 OBTENEMOS EL CORE DE FORMA SEGURA (Paper 1.21.5)
        NexoCore corePlugin = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (corePlugin == null) {
            getLogger().severe("❌ FATAL: NexoCore no detectado. Apagando el módulo de Minions...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 1. Inicializar Inyección como Child Injector
        // Permite acceder a DatabaseManager y CrossplayUtils sin duplicar conexiones
        this.childInjector = corePlugin.getInjector().createChildInjector(new MinionsModule(this));

        // 🚀 2. Arrancar Orquestador Inyectado
        // (El Bootstrap se encarga de forzar la carga en RAM de las configs y managers)
        this.bootstrap = childInjector.getInstance(MinionsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡NexoMinions cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
        getLogger().info("🤖 NexoMinions apagado de forma segura.");
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
    // Guice ya gestiona la caché O(1) internamente.
    // ==========================================

    @Deprecated
    public MinionManager getMinionManager() { return childInjector.getInstance(MinionManager.class); }

    @Deprecated
    public TiersConfig getTiersConfig() { return childInjector.getInstance(TiersConfig.class); }

    @Deprecated
    public UpgradesConfig getUpgradesConfig() { return childInjector.getInstance(UpgradesConfig.class); }

    @Deprecated
    public ConfigManager getConfigManager() { return childInjector.getInstance(ConfigManager.class); }
}
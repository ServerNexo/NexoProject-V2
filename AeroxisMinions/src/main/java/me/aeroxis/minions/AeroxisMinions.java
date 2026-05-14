package me.aeroxis.minions;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.islas.AeroxisIslas;
import me.aeroxis.minions.config.ConfigManager;
import me.aeroxis.minions.data.TiersConfig;
import me.aeroxis.minions.data.UpgradesConfig;
import me.aeroxis.minions.di.MinionsModule;
import me.aeroxis.minions.manager.MinionManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 🤖 AeroxisMinions - Clase Principal (Arquitectura Enterprise)
 * Rendimiento: Child Injector (Guice) heredado del Core y Orquestador de Servicios.
 */
public class AeroxisMinions extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    private MinionsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🤖 Iniciando AeroxisMinions (Motor Enterprise)...");

        // 🌟 OBTENEMOS EL CORE DE FORMA SEGURA (Paper 1.21.5)
        AeroxisCore corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ FATAL: AeroxisCore no detectado. Apagando el módulo de Minions...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 FIX CRÍTICO: Verificamos que AeroxisIslas esté encendido y usando el nuevo getInjector()
        AeroxisIslas islasPlugin = (AeroxisIslas) getServer().getPluginManager().getPlugin("AeroxisIslas");
        if (islasPlugin == null || islasPlugin.getInjector() == null) {
            getLogger().severe("❌ FATAL: AeroxisIslas no está operativo. AeroxisMinions requiere que las Islas estén encendidas primero.");
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

        getLogger().info("✅ ¡AeroxisMinions cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
        getLogger().info("🤖 AeroxisMinions apagado de forma segura.");
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
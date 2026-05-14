package me.aeroxis.protections;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.protections.config.ConfigManager;
import me.aeroxis.protections.di.ProtectionsModule;
import me.aeroxis.protections.managers.ClaimManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 🛡️ AeroxisProtections - Clase Principal (Arquitectura Enterprise)
 * Inyección a través de ChildInjector para heredar dependencias del Core.
 */
public class AeroxisProtections extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    private ProtectionsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🛡️ Sincronizando AeroxisProtections con el Core Engine...");

        // 🌟 1. OBTENEMOS EL INYECTOR MAESTRO DEL CORE (Forma 100% segura para Paper)
        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error crítico: AeroxisCore no encontrado. Apagando submódulo.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Injector coreInjector = corePlugin.getInjector();

        // 🌟 2. CREAMOS EL INYECTOR HIJO (Hereda la DB, Usuarios, Economía, etc.)
        this.childInjector = coreInjector.createChildInjector(new ProtectionsModule(this));

        // 🚀 3. Arrancar Orquestador
        this.bootstrap = childInjector.getInstance(ProtectionsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡AeroxisProtections cargado y operativo!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🛡️ Apagando sistemas de protección...");
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // 🌟 Comando para recargar (usado de forma segura desde ComandoProteccion.java)
    public void reloadSystem() {
        getLogger().info("🔄 Recargando AeroxisProtections...");

        ClaimManager claimManager = childInjector.getInstance(ClaimManager.class);
        claimManager.getAllStones().clear();
        claimManager.loadAllStonesAsync(); // Ejecución asíncrona mediante Virtual Threads interna

        // 💡 También recargamos los textos
        childInjector.getInstance(ConfigManager.class).reloadMessages();
    }

    /**
     * 🌉 PUENTE DE ARQUITECTURA
     * Permite que otros módulos (como NexoWar o NexoPvP) obtengan instancias
     * de Protecciones sin crear duplicados que rompan Guice.
     */
    // 🌟 FIX DEFINITIVO: Renombramos a getInjector() para estandarizar todo el ecosistema
    public Injector getInjector() {
        return childInjector;
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS (PUENTE LEGACY)
    // ==========================================

    @Deprecated
    public ConfigManager getConfigManager() {
        return childInjector.getInstance(ConfigManager.class);
    }

    @Deprecated
    public ClaimManager getClaimManager() {
        return childInjector.getInstance(ClaimManager.class);
    }
}
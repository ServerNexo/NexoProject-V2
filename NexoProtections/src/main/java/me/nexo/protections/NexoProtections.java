package me.nexo.protections;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.di.ProtectionsModule;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 🛡️ NexoProtections - Clase Principal (Arquitectura Enterprise)
 * Inyección a través de ChildInjector para heredar dependencias del Core.
 */
public class NexoProtections extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    private ProtectionsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🛡️ Sincronizando NexoProtections con el Core Engine...");

        // 🌟 1. OBTENEMOS EL INYECTOR MAESTRO DEL CORE
        Injector coreInjector = NexoCore.getInstance().getInjector();
        
        // 🌟 2. CREAMOS EL INYECTOR HIJO (Hereda la DB, Usuarios, Economía, etc.)
        this.childInjector = coreInjector.createChildInjector(new ProtectionsModule(this));

        // 🚀 3. Arrancar Orquestador
        this.bootstrap = childInjector.getInstance(ProtectionsBootstrap.class);
        this.bootstrap.startServices();

        getLogger().info("✅ ¡NexoProtections cargado y operativo!");
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
        getLogger().info("🔄 Recargando NexoProtections...");

        ClaimManager claimManager = childInjector.getInstance(ClaimManager.class);
        claimManager.getAllStones().clear();
        claimManager.loadAllStonesAsync(); // Ejecución asíncrona mediante Virtual Threads interna

        // 💡 También recargamos los textos
        childInjector.getInstance(ConfigManager.class).reloadMessages();
    }

    public Injector getChildInjector() {
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
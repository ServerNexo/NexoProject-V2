package me.aeroxis.islas;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.islas.api.AeroxisIslasExpansion; // 🌟 IMPORT NUEVO
import me.aeroxis.islas.commands.ComandoIsla;
import me.aeroxis.islas.di.IslasModule;
// import listeners.me.aeroxis.islas.IslandListener; // Descomenta si lo tienes
import me.aeroxis.islas.listeners.IslandSecurityListener;
import me.aeroxis.islas.listeners.IslandProgressionListener;
import me.aeroxis.islas.managers.IslandLevelEngine; // 🌟 IMPORT NUEVO
import me.aeroxis.islas.managers.IslandManager;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏝️ AeroxisIslas - Motor de Skyblock MMO (Arquitectura Grid Nativa)
 */
public class AeroxisIslas extends JavaPlugin {

    private Injector childInjector;
    private BukkitCommandHandler commandHandler;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏝️ Iniciando AeroxisIslas (Grid Nativo)...");

        // 🌟 FIX BUG 1: Generar el config.yml físico para que el Motor de Niveles lea la XP
        saveDefaultConfig();

        // 🛡️ 1. Verificar Core
        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: Falta AeroxisCore. Apagando AeroxisIslas.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 2. Inicializar Guice (Módulo)
        this.childInjector = corePlugin.getInjector().createChildInjector(new IslasModule(this));

        // 🌟 3. Registrar Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(IslandSecurityListener.class), this);
        pm.registerEvents(childInjector.getInstance(IslandProgressionListener.class), this);

        // 🎮 4. Registrar Comando /is (Lamp)
        this.commandHandler = BukkitCommandHandler.create(this);
        this.commandHandler.register(childInjector.getInstance(ComandoIsla.class));

        // 🌟 5. Registrar PlaceholderAPI (El Puente para el TAB y Hologramas)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AeroxisIslasExpansion(
                    this,
                    childInjector.getInstance(IslandManager.class),
                    childInjector.getInstance(IslandLevelEngine.class)
            ).register();
            getLogger().info("✅ Hook con PlaceholderAPI establecido exitosamente.");
        } else {
            getLogger().warning("⚠️ PlaceholderAPI no encontrado. Las variables %nexoislas_...% no funcionarán.");
        }

        getLogger().info("✅ AeroxisIslas en línea. ¡Listo para generar parcelas!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏝️ Apagando AeroxisIslas...");

        // 🌟 FIX BUG 2: Ejecutar el protocolo de Apagado Seguro antes de matar la RAM
        if (this.childInjector != null) {
            try {
                IslandManager manager = childInjector.getInstance(IslandManager.class);
                manager.shutdownSafely();
            } catch (Exception e) {
                getLogger().severe("❌ Error durante el guardado de emergencia: " + e.getMessage());
            }
        }

        if (this.commandHandler != null) {
            this.commandHandler.unregisterAllCommands();
        }
    }

    public Injector getInjector() {
        return childInjector;
    }
}
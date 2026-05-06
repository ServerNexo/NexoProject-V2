package me.nexo.islas;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.islas.commands.ComandoIsla;
import me.nexo.islas.di.IslasModule;
// import me.nexo.islas.listeners.IslandListener; // Descomenta si lo tienes
import me.nexo.islas.listeners.IslandSecurityListener;
import me.nexo.islas.listeners.IslandProgressionListener; // 🌟 EL NUEVO MOTOR AKUMA
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏝️ NexoIslas - Motor de Skyblock MMO (Arquitectura Grid Nativa)
 */
public class NexoIslas extends JavaPlugin {

    private Injector childInjector;
    private BukkitCommandHandler commandHandler;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏝️ Iniciando NexoIslas (Grid Nativo)...");

        // 🛡️ 1. Verificar Core
        var corePlugin = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: Falta NexoCore. Apagando NexoIslas.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 2. Inicializar Guice (Módulo)
        this.childInjector = corePlugin.getInjector().createChildInjector(new IslasModule(this));

        // 🌟 3. Registrar Listeners (🌟 FIX: Usando childInjector correctamente)
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(IslandSecurityListener.class), this);
        pm.registerEvents(childInjector.getInstance(IslandProgressionListener.class), this);

        // 🎮 4. Registrar Comando /is (Lamp)
        this.commandHandler = BukkitCommandHandler.create(this);
        this.commandHandler.register(childInjector.getInstance(ComandoIsla.class));

        getLogger().info("✅ NexoIslas en línea. ¡Listo para generar parcelas!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏝️ Apagando NexoIslas...");
        if (this.commandHandler != null) {
            this.commandHandler.unregisterAllCommands();
        }
    }

    // 🌟 FIX: Renombramos el método a getInjector() para estandarizarlo con NexoFactories, Minions y Colecciones
    public Injector getInjector() {
        return childInjector;
    }
}
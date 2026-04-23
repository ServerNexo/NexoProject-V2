package me.nexo.clans;

import com.google.inject.Injector;
import me.nexo.clans.commands.ComandoChatClan;
import me.nexo.clans.commands.ComandoClan;
import me.nexo.clans.config.ConfigManager;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.di.ClansModule;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.listeners.ClanDamageListener;
import me.nexo.core.NexoCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * 👥 NexoClans - Motor de Gremios (Arquitectura Enterprise)
 * Totalmente inyectado y acoplado al CommandMap Nativo de PaperMC.
 */
public class NexoClans extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;
    
    private ConfigManager configManager;
    private ClanManager clanManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("👥 Sincronizando NexoClans con el Core Engine...");

        // 🌟 1. OBTENEMOS EL INYECTOR MAESTRO DEL CORE
        Injector coreInjector = NexoCore.getInstance().getInjector();

        // 🌟 2. CREAMOS EL INYECTOR HIJO (Hereda la DB, Usuarios, etc.)
        this.childInjector = coreInjector.createChildInjector(new ClansModule(this));

        // 🌟 3. OBTENEMOS MANAGERS (Inyectados automáticamente)
        this.configManager = childInjector.getInstance(ConfigManager.class);
        this.clanManager = childInjector.getInstance(ClanManager.class);

        // 🌟 4. REGISTRAMOS EVENTOS INYECTADOS
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(ClanConnectionListener.class), this);
        pm.registerEvents(childInjector.getInstance(ClanDamageListener.class), this);

        // 🌟 5. INYECCIÓN NATIVA DE COMANDOS (Paper 1.21.5+)
        // Importante: Las clases de comando ahora deben extender org.bukkit.command.Command
        registerNativeCommand((Command) childInjector.getInstance(ComandoClan.class));
        registerNativeCommand((Command) childInjector.getInstance(ComandoChatClan.class));

        getLogger().info("✅ NexoClans habilitado y conectado a la red social.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("👥 Guardando progreso y bases de los Clanes...");

        // 🌟 Guardado Síncrono Seguro para evitar corrupción al apagar el server (Regla 3)
        if (clanManager != null) {
            try {
                clanManager.saveAllClansSync();
            } catch (Exception e) {
                getLogger().severe("❌ Error forzando el guardado final de clanes: " + e.getMessage());
            }
        }

        getLogger().info("✅ NexoClans apagado de forma segura.");
    }

    /**
     * 🚀 INYECCIÓN NATIVA EN EL COMMAND MAP (Evita el bloqueo de JavaPlugin#getCommand)
     */
    private void registerNativeCommand(Command command) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            
            // Registramos el comando con el prefijo "nexoclans:" como fallback
            commandMap.register(getName().toLowerCase(), command);
        } catch (Exception e) {
            getLogger().severe("❌ Error inyectando el comando " + command.getName() + " en el CommandMap: " + e.getMessage());
        }
    }

    // ==========================================================
    // 🌐 MÉTODOS DE API EXTERNA / PUENTE LEGACY
    // ==========================================================

    @Deprecated
    public ConfigManager getConfigManager() { return configManager; }
    @Deprecated
    public ClanManager getClanManager() { return clanManager; }

    public Injector getChildInjector() { return childInjector; }
}
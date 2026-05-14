package me.aeroxis.clans;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.clans.commands.ComandoChatClan;
import me.aeroxis.clans.commands.ComandoClan;
import me.aeroxis.clans.config.ConfigManager;
import me.aeroxis.clans.core.ClanManager;
import me.aeroxis.clans.di.ClansModule;
import me.aeroxis.clans.listeners.ClanConnectionListener;
import me.aeroxis.clans.listeners.ClanDamageListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * 👥 AeroxisClans - Motor de Gremios (Arquitectura Enterprise)
 * Totalmente inyectado y acoplado al CommandMap Nativo de PaperMC.
 */
public class AeroxisClans extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;

    private ConfigManager configManager;
    private ClanManager clanManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("👥 Sincronizando AeroxisClans con el Core Engine...");

        // 🌟 1. OBTENEMOS EL CORE DE FORMA SEGURA (Sin métodos estáticos obsoletos)
        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: AeroxisCore no detectado. Apagando AeroxisClans...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Extraemos el inyector maestro
        Injector coreInjector = corePlugin.getInjector();

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

        getLogger().info("✅ AeroxisClans habilitado y conectado a la red social.");
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

        getLogger().info("✅ AeroxisClans apagado de forma segura.");
    }

    /**
     * 🚀 INYECCIÓN NATIVA EN EL COMMAND MAP (Evita el bloqueo de JavaPlugin#getCommand)
     */
    private void registerNativeCommand(Command command) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            // Registramos el comando con el prefijo "aeroxisclans:" como fallback
            commandMap.register(getName().toLowerCase(), command);
        } catch (Exception e) {
            getLogger().severe("❌ Error inyectando el comando " + command.getName() + " en el CommandMap: " + e.getMessage());
        }
    }

    // 🌟 FIX DEFINITIVO: Renombramos a getInjector() para estandarizar el ecosistema y evitar crasheos de inyección
    public Injector getInjector() { return childInjector; }

    // ==========================================================
    // 🌐 MÉTODOS DE API EXTERNA / PUENTE LEGACY
    // ==========================================================

    @Deprecated
    public ConfigManager getConfigManager() { return configManager; }
    @Deprecated
    public ClanManager getClanManager() { return clanManager; }
}
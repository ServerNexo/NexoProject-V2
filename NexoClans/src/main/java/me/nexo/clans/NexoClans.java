package me.nexo.clans;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.clans.commands.ComandoChatClan;
import me.nexo.clans.commands.ComandoClan;
import me.nexo.clans.commands.ComandoClanTabCompleter;
import me.nexo.clans.config.ConfigManager;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.di.ClansModule;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.listeners.ClanDamageListener;
import me.nexo.core.user.NexoAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 👥 NexoClans - Clase Principal (Arquitectura Enterprise)
 */
public class NexoClans extends JavaPlugin {

    private Injector injector;

    private ConfigManager configManager;
    private ClanManager clanManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("👥 Iniciando NexoClans (Motor Enterprise)...");

        // 🛡️ Blindaje: Asegurarnos de que el Core está encendido
        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ FATAL: NexoCore no encontrado. El sistema de clanes se apagará.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 1. INICIALIZAMOS GUICE (El corazón de la inyección de dependencias)
        this.injector = Guice.createInjector(new ClansModule(this));

        // 🌟 2. OBTENEMOS MANAGERS (Inyectados automáticamente)
        this.configManager = injector.getInstance(ConfigManager.class);
        this.clanManager = injector.getInstance(ClanManager.class);

        // 🌟 3. REGISTRAMOS EN LA API GLOBAL DE NEXO
        NexoAPI.getServices().register(ClanManager.class, this.clanManager);

        // 🌟 4. REGISTRAMOS EVENTOS INYECTADOS
        getServer().getPluginManager().registerEvents(injector.getInstance(ClanConnectionListener.class), this);
        getServer().getPluginManager().registerEvents(injector.getInstance(ClanDamageListener.class), this);

        // 🌟 5. REGISTRAMOS COMANDOS INYECTADOS
        if (getCommand("clan") != null) {
            getCommand("clan").setExecutor(injector.getInstance(ComandoClan.class));
            // El TabCompleter no requiere inyección al no tener dependencias pesadas
            getCommand("clan").setTabCompleter(new ComandoClanTabCompleter());
        }
        if (getCommand("c") != null) {
            getCommand("c").setExecutor(injector.getInstance(ComandoChatClan.class));
        }

        getLogger().info("✅ NexoClans habilitado y conectado a la red social.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("👥 Guardando progreso y bases de los Clanes...");

        // 🌟 Guardado Síncrono Seguro para evitar corrupción al apagar el server
        if (clanManager != null) {
            try {
                clanManager.saveAllClansSync();
            } catch (Exception e) {
                getLogger().severe("❌ Error forzando el guardado final de clanes: " + e.getMessage());
            }
        }

        NexoAPI.getServices().unregister(ClanManager.class);
        getLogger().info("✅ NexoClans apagado de forma segura.");
    }

    // 🌟 Getters de compatibilidad temporal
    public ConfigManager getConfigManager() { return configManager; }
    public ClanManager getClanManager() { return clanManager; }
}
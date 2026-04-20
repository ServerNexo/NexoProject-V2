package me.nexo.clans;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.clans.commands.ComandoChatClan;
import me.nexo.clans.commands.ComandoClan;
import me.nexo.clans.config.ConfigManager;
import me.nexo.clans.core.ClanManager;
import me.nexo.clans.di.ClansModule;
import me.nexo.clans.listeners.ClanConnectionListener;
import me.nexo.clans.listeners.ClanDamageListener;
import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoAPI;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * 👥 NexoClans - Clase Principal (Arquitectura NATIVA + Anti-Race Conditions)
 */
public class NexoClans extends JavaPlugin {

    private Injector injector;

    private ConfigManager configManager;
    private ClanManager clanManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("👥 Pre-iniciando NexoClans (Esperando enlace seguro con el Core)...");

        // 🛡️ Blindaje: Asegurarnos de que el Core está encendido
        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ FATAL: NexoCore no encontrado. El sistema de clanes se apagará.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 EL CANDADO: Esperamos a que NexoCore confirme que la BD y su Guice están listos
        NexoCore.getInstance().getCoreReadyFuture().thenRun(() -> {

            getLogger().info("🔓 Luz verde recibida de NexoCore. Arrancando motor de clanes...");

            // 🌟 1. INICIALIZAMOS GUICE (Seguro)
            this.injector = Guice.createInjector(new ClansModule(this));

            // 🌟 2. OBTENEMOS MANAGERS (Inyectados automáticamente)
            this.configManager = injector.getInstance(ConfigManager.class);
            this.clanManager = injector.getInstance(ClanManager.class);

            // 🌟 3. REGISTRAMOS EN LA API GLOBAL DE NEXO
            NexoAPI.getServices().register(ClanManager.class, this.clanManager);

            // 🌟 4. REGISTRAMOS EVENTOS INYECTADOS
            getServer().getPluginManager().registerEvents(injector.getInstance(ClanConnectionListener.class), this);
            getServer().getPluginManager().registerEvents(injector.getInstance(ClanDamageListener.class), this);

            // 🌟 FIX: 5. INYECCIÓN DE COMANDOS NATIVOS POR REFLEXIÓN
            // Esto evita que PaperMC bloquee el arranque por no usar su sistema de eventos nativo.
            try {
                Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

                // 💉 Le pedimos a Guice nuestras clases inyectadas y las registramos a la fuerza
                commandMap.register("nexoclans", injector.getInstance(ComandoClan.class));
                commandMap.register("nexoclans", injector.getInstance(ComandoChatClan.class));

                getLogger().info("✅ Comandos Nativos inyectados con éxito (Zero-Lag)");
            } catch (Exception e) {
                getLogger().severe("❌ Error inyectando comandos en el CommandMap: " + e.getMessage());
                e.printStackTrace();
            }

            getLogger().info("✅ NexoClans habilitado y conectado a la red social.");
            getLogger().info("========================================");

        }).exceptionally(ex -> {
            getLogger().severe("❌ Error fatal esperando al Core en NexoClans: " + ex.getMessage());
            return null;
        });
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
            NexoAPI.getServices().unregister(ClanManager.class);
        }

        getLogger().info("✅ NexoClans apagado de forma segura.");
    }

    // 🌟 Getters de compatibilidad temporal (Devolverán null de forma segura si se piden antes de que abra el candado)
    public ConfigManager getConfigManager() { return configManager; }
    public ClanManager getClanManager() { return clanManager; }
}
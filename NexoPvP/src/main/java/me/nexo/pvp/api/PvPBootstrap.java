package me.nexo.pvp.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.classes.ArmorClassListener;
import me.nexo.pvp.commands.ComandoTemplo;
import me.nexo.pvp.mechanics.DeathPenaltyListener;
import me.nexo.pvp.mechanics.TrainingStationListener;
import me.nexo.pvp.pasivas.PasivasListener;
import me.nexo.pvp.pvp.ComandoPvP;
import me.nexo.pvp.pvp.PvPListener;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

@Singleton
public class PvPBootstrap {

    private final NexoPvP plugin;
    private final Server server;
    private final Injector injector;

    // 💉 PILAR 3: Inyección Limpia (Solo pedimos lo que usamos)
    @Inject
    public PvPBootstrap(NexoPvP plugin, Injector injector) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoPvP Enterprise (NATIVA)");

        registerEvents();
        // 🌟 FIX: Registro de comandos por Magia Negra
        registerCommands();

        plugin.getLogger().info("⚔️ NexoPvP activado e inyectado con éxito.");
    }

    public void stopServices() {
        plugin.getLogger().info("⚔️ NexoPvP apagado.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        // 🚀 Eventos Purificados y Desacoplados
        pm.registerEvents(injector.getInstance(PvPListener.class), plugin);
        pm.registerEvents(injector.getInstance(PasivasListener.class), plugin);
        pm.registerEvents(injector.getInstance(ArmorClassListener.class), plugin);
        pm.registerEvents(injector.getInstance(DeathPenaltyListener.class), plugin);
        pm.registerEvents(injector.getInstance(TrainingStationListener.class), plugin);
    }

    private void registerCommands() {
        try {
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(server);

            // 💉 Inyectamos los comandos saltándonos la seguridad estricta de Paper
            commandMap.register("nexopvp", injector.getInstance(ComandoPvP.class));
            commandMap.register("nexopvp", injector.getInstance(ComandoTemplo.class));

            plugin.getLogger().info("✅ Comandos de NexoPvP inyectados nativamente (Zero-Lag).");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error inyectando comandos de NexoPvP: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
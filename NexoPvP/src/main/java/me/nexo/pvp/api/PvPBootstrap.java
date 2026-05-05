package me.nexo.pvp.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.classes.ArmorClassListener;
import me.nexo.pvp.combat.CombatClickListener; // 🌟 NUEVO
import me.nexo.pvp.combat.PoiseManager; // 🌟 Motor inyectado
import me.nexo.pvp.commands.ComandoTemplo;
import me.nexo.pvp.mechanics.DeathPenaltyListener;
import me.nexo.pvp.mechanics.TrainingStationListener;
import me.nexo.pvp.pasivas.PasivasListener;
import me.nexo.pvp.pasivas.PasivasManager; // 🌟 Motor inyectado
import me.nexo.pvp.pvp.ComandoPvP;
import me.nexo.pvp.pvp.PvPListener;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoPvP - Orquestador Central (Arquitectura Enterprise)
 * Enciende, registra y apaga todos los servicios inyectados del módulo.
 */
@Singleton
public class PvPBootstrap {

    private final NexoPvP plugin;
    private final Server server;
    private final Injector injector;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada
    private final PasivasManager pasivasManager;
    private final PoiseManager poiseManager;

    // 💉 PILAR 1: Inyección Limpia
    @Inject
    public PvPBootstrap(NexoPvP plugin, Injector injector, CrossplayUtils crossplayUtils,
                        PasivasManager pasivasManager, PoiseManager poiseManager) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
        this.crossplayUtils = crossplayUtils;
        this.pasivasManager = pasivasManager;
        this.poiseManager = poiseManager;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoPvP Enterprise...");

        // 🚀 1. ARRANCAMOS LOS MOTORES QUE SACAMOS DE LOS CONSTRUCTORES
        // Esto resuelve el error de AuraSkills y el warning [this-escape]
        pasivasManager.initialize();
        poiseManager.start();

        // 2. Registramos todo lo demás
        registerEvents();
        registerCommands();

        plugin.getLogger().info("✅ NexoPvP activado e inyectado con éxito.");
    }

    public void stopServices() {
        plugin.getLogger().info("⚔️ NexoPvP apagado de forma segura.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        // 🚀 Eventos Purificados y Desacoplados (Resolución automática por Guice)
        pm.registerEvents(injector.getInstance(PvPListener.class), plugin);
        pm.registerEvents(injector.getInstance(PasivasListener.class), plugin);
        pm.registerEvents(injector.getInstance(ArmorClassListener.class), plugin);
        pm.registerEvents(injector.getInstance(DeathPenaltyListener.class), plugin);
        pm.registerEvents(injector.getInstance(TrainingStationListener.class), plugin);

        // ⚔️ EVENTOS DE COMBATE TÁCTICO
        pm.registerEvents(injector.getInstance(CombatClickListener.class), plugin); // 🌟 NUEVO
    }

    private void registerCommands() {
        // 💡 Inicializamos el motor de Lamp para NexoPvP
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // 🛡️ CONTROL GLOBAL DE PERMISOS (Modernizado a Paper 1.21.5)
        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {

            // 🌟 FIX: Casteo seguro a BukkitCommandActor para habilitar isPlayer() y getAsPlayer()
            BukkitCommandActor bukkitActor = (BukkitCommandActor) actor;

            // 🌟 Integramos CrossplayUtils para Bedrock/Java.
            if (bukkitActor.isPlayer()) {
                crossplayUtils.sendMessage(bukkitActor.getAsPlayer(), "&#FF5555[!] No tienes autorización táctica para este comando.");
            } else {
                bukkitActor.error("❌ No tienes autorización táctica para este comando.");
            }
        });

        // 💉 Usamos a Guice para construir e inyectar los comandos a la memoria de Lamp
        handler.register(injector.getInstance(ComandoPvP.class));
        handler.register(injector.getInstance(ComandoTemplo.class));
    }
}
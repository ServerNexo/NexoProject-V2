package me.aeroxis.protections;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.protections.commands.ComandoProteccion;
import me.aeroxis.protections.listeners.EnvironmentListener;
import me.aeroxis.protections.listeners.ProtectionListener;
import me.aeroxis.protections.managers.ClaimManager;
import me.aeroxis.protections.managers.UpkeepManager;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandActor;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ AeroxisProtections - Orquestador Enterprise
 * Rendimiento: Inyección Pura, Cero llamadas estáticas API y Modernización de Mensajes.
 */
@Singleton
public class ProtectionsBootstrap {

    private final AeroxisProtections plugin;
    private final Server server;
    private final Injector injector;
    private final ClaimManager claimManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia Inyectada

    // 💉 PILAR 1: Inyección Limpia
    @Inject
    public ProtectionsBootstrap(AeroxisProtections plugin, Injector injector, ClaimManager claimManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
        this.claimManager = claimManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura AeroxisProtections Enterprise...");

        // 🌟 Cargar datos de la BD asíncronamente (Virtual Threads en el Manager)
        claimManager.loadAllStonesAsync();

        // 🌟 Forzar el arranque del Mantenimiento de Energía
        // (Nota: Si UpkeepManager está como asEagerSingleton() en ProtectionsModule, esta línea se puede omitir).
        injector.getInstance(UpkeepManager.class);

        registerEvents();
        registerCommands();

        plugin.getLogger().info("🛡️ AeroxisProtections activado e inyectado con éxito.");
    }

    public void stopServices() {
        // 🌟 Eliminado: AeroxisAPI.getServices().unregister(ClaimManager.class); (Innecesario con Guice)
        plugin.getLogger().info("🛡️ AeroxisProtections apagado. La energía fue guardada asíncronamente.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();
        // 🚀 Eventos Purificados y Desacoplados
        pm.registerEvents(injector.getInstance(ProtectionListener.class), plugin);
        pm.registerEvents(injector.getInstance(EnvironmentListener.class), plugin);
    }

    private void registerCommands() {
        // 💡 Inicializamos el motor de Lamp para AeroxisProtections
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // 🛡️ CONTROL GLOBAL DE PERMISOS (Modernizado a Paper 1.21.5)
        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {

            // 🌟 FIX: Casteo seguro a BukkitCommandActor para habilitar isPlayer() y getAsPlayer()
            BukkitCommandActor bukkitActor = (BukkitCommandActor) actor;

            // 🌟 Integramos CrossplayUtils para Bedrock/Java.
            if (bukkitActor.isPlayer()) {
                crossplayUtils.sendMessage(bukkitActor.getAsPlayer(), "&#FF5555❌ El Vacío rechaza tu petición (Sin Permisos).");
            } else {
                bukkitActor.error("❌ El Vacío rechaza tu petición (Sin Permisos).");
            }
        });

        handler.register(injector.getInstance(ComandoProteccion.class));
    }
}
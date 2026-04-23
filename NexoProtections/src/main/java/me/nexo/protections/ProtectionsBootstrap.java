package me.nexo.protections;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.commands.ComandoProteccion;
import me.nexo.protections.listeners.EnvironmentListener;
import me.nexo.protections.listeners.ProtectionListener;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.protections.managers.UpkeepManager;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoProtections - Orquestador Enterprise
 * Rendimiento: Inyección Pura, Cero llamadas estáticas API y Modernización de Mensajes.
 */
@Singleton
public class ProtectionsBootstrap {

    private final NexoProtections plugin;
    private final Server server;
    private final Injector injector;
    private final ClaimManager claimManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia Inyectada

    // 💉 PILAR 1: Inyección Limpia
    @Inject
    public ProtectionsBootstrap(NexoProtections plugin, Injector injector, ClaimManager claimManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
        this.claimManager = claimManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoProtections Enterprise...");

        // 🌟 Cargar datos de la BD asíncronamente (Virtual Threads en el Manager)
        claimManager.loadAllStonesAsync();

        // 🌟 Forzar el arranque del Mantenimiento de Energía
        // (Nota: Si UpkeepManager está como asEagerSingleton() en ProtectionsModule, esta línea se puede omitir).
        injector.getInstance(UpkeepManager.class);

        registerEvents();
        registerCommands();

        plugin.getLogger().info("🛡️ NexoProtections activado e inyectado con éxito.");
    }

    public void stopServices() {
        // 🌟 Eliminado: NexoAPI.getServices().unregister(ClaimManager.class); (Innecesario con Guice)
        plugin.getLogger().info("🛡️ NexoProtections apagado. La energía fue guardada asíncronamente.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();
        // 🚀 Eventos Purificados y Desacoplados
        pm.registerEvents(injector.getInstance(ProtectionListener.class), plugin);
        pm.registerEvents(injector.getInstance(EnvironmentListener.class), plugin);
    }

    private void registerCommands() {
        // 💡 Inicializamos el motor de Lamp para NexoProtections
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        // 🛡️ CONTROL GLOBAL DE PERMISOS (Modernizado a Paper 1.21.5)
        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            // 🌟 FIX: Adiós al obsoleto §c. Integramos CrossplayUtils para Bedrock/Java.
            if (actor.isPlayer()) {
                crossplayUtils.sendMessage((Player) actor.getSender(), "&#FF5555❌ El Vacío rechaza tu petición (Sin Permisos).");
            } else {
                actor.error("❌ El Vacío rechaza tu petición (Sin Permisos).");
            }
        });

        handler.register(injector.getInstance(ComandoProteccion.class));
    }
}
package me.nexo.protections;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.core.user.NexoAPI;
import me.nexo.protections.commands.ComandoProteccion;
import me.nexo.protections.listeners.EnvironmentListener;
import me.nexo.protections.listeners.ProtectionListener;
import me.nexo.protections.managers.ClaimManager;
import me.nexo.protections.managers.UpkeepManager;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoProtections - Orquestador Enterprise
 */
@Singleton
public class ProtectionsBootstrap {

    private final NexoProtections plugin;
    private final Server server;
    private final Injector injector;
    private final ClaimManager claimManager;

    @Inject
    public ProtectionsBootstrap(NexoProtections plugin, Injector injector, ClaimManager claimManager) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
        this.claimManager = claimManager;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoProtections Enterprise");

        // 🌟 Registrar el API y cargar datos de la BD asíncronamente
        NexoAPI.getServices().register(ClaimManager.class, claimManager);
        claimManager.loadAllStonesAsync();

        // 🌟 Forzar el arranque del Mantenimiento de Energía
        injector.getInstance(UpkeepManager.class);

        registerEvents();
        registerCommands();

        plugin.getLogger().info("🛡️ NexoProtections activado e inyectado con éxito.");
    }

    public void stopServices() {
        NexoAPI.getServices().unregister(ClaimManager.class);
        plugin.getLogger().info("🛡️ NexoProtections apagado. La energía fue guardada asíncronamente.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();
        pm.registerEvents(injector.getInstance(ProtectionListener.class), plugin);
        pm.registerEvents(injector.getInstance(EnvironmentListener.class), plugin);
    }

    private void registerCommands() {
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error("§c❌ El Vacío rechaza tu petición (Sin Permisos).");
        });

        handler.register(injector.getInstance(ComandoProteccion.class));
    }
}
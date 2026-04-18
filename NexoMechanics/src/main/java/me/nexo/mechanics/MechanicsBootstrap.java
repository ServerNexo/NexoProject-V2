package me.nexo.mechanics;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.minigames.AlchemyMinigameManager;
import me.nexo.mechanics.minigames.CombatComboManager;
import me.nexo.mechanics.minigames.EnchantingMinigameManager;
import me.nexo.mechanics.minigames.FarmingMinigameManager;
import me.nexo.mechanics.minigames.FishingHookManager;
import me.nexo.mechanics.minigames.MiningMinigameManager;
import me.nexo.mechanics.minigames.WoodcuttingMinigameManager;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoMechanics - Orquestador Enterprise
 */
@Singleton
public class MechanicsBootstrap {

    private final NexoMechanics plugin;
    private final Server server;
    private final Injector injector;

    @Inject
    public MechanicsBootstrap(NexoMechanics plugin, Injector injector) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoMechanics Enterprise");

        registerEvents();
        registerCommands();

        plugin.getLogger().info("⚙️ NexoMechanics activado e inyectado con éxito.");
    }

    public void stopServices() {
        plugin.getLogger().info("⚙️ NexoMechanics apagado.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        // 🌟 Registro Inyectado de los Minijuegos
        pm.registerEvents(injector.getInstance(AlchemyMinigameManager.class), plugin);
        pm.registerEvents(injector.getInstance(CombatComboManager.class), plugin);
        pm.registerEvents(injector.getInstance(EnchantingMinigameManager.class), plugin);
        pm.registerEvents(injector.getInstance(FarmingMinigameManager.class), plugin);
        pm.registerEvents(injector.getInstance(FishingHookManager.class), plugin);
        pm.registerEvents(injector.getInstance(MiningMinigameManager.class), plugin);
        pm.registerEvents(injector.getInstance(WoodcuttingMinigameManager.class), plugin);
    }

    private void registerCommands() {
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error(plugin.getConfigManager().getMessages().mensajes().errores().sinPermiso());
        });

        handler.register(injector.getInstance(ComandoSkillTree.class));
    }
}
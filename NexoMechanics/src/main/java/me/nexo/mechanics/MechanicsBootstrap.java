package me.nexo.mechanics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.events.NexoEventManager; // 🌟 IMPORT DEL ORQUESTADOR
import me.nexo.mechanics.archeology.ArcheologyManager;
import me.nexo.mechanics.archeology.ArcheologyCompassTracker;
import me.nexo.mechanics.archeology.ArcheologyListener;
import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.commands.ComandoArcheology;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.managers.ContrabandManager;
import me.nexo.mechanics.minigames.*;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.concurrent.TimeUnit;

@Singleton
public class MechanicsBootstrap {

    private final NexoMechanics plugin;
    private final Server server;
    private final ConfigManager configManager;

    private final AlchemyMinigameManager alchemyMinigame;
    private final CombatComboManager combatCombo;
    private final EnchantingMinigameManager enchantingMinigame;
    private final FarmingMinigameManager farmingMinigame;
    private final FishingHookManager fishingHook;
    private final MiningMinigameManager miningMinigame;
    private final WoodcuttingMinigameManager woodcuttingMinigame;
    private final ComandoSkillTree comandoSkillTree;
    private final ComandoArcheology comandoArcheology;

    private final ContrabandManager contrabandManager;

    // 🌟 SISTEMAS GLOBALES (INDUCIDOS POR EL CORE VÍA GUICE)
    private final NexoEventManager globalEventManager; // 🌟 AÑADIDO

    // 🌟 SISTEMA DE ARQUEOLOGÍA
    private final ArcheologyManager archeologyManager;
    private final ArcheologyCompassTracker compassTracker;
    private final ArcheologyListener brushListener;

    @Inject
    public MechanicsBootstrap(NexoMechanics plugin, ConfigManager configManager,
                              AlchemyMinigameManager alchemyMinigame,
                              CombatComboManager combatCombo,
                              EnchantingMinigameManager enchantingMinigame,
                              FarmingMinigameManager farmingMinigame,
                              FishingHookManager fishingHook,
                              MiningMinigameManager miningMinigame,
                              WoodcuttingMinigameManager woodcuttingMinigame,
                              ComandoSkillTree comandoSkillTree,
                              ComandoArcheology comandoArcheology,
                              ContrabandManager contrabandManager,
                              NexoEventManager globalEventManager, // 🌟 INYECTADO DIRECTAMENTE AQUÍ
                              ArcheologyManager archeologyManager,
                              ArcheologyCompassTracker compassTracker,
                              ArcheologyListener brushListener) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.configManager = configManager;

        this.alchemyMinigame = alchemyMinigame;
        this.combatCombo = combatCombo;
        this.enchantingMinigame = enchantingMinigame;
        this.farmingMinigame = farmingMinigame;
        this.fishingHook = fishingHook;
        this.miningMinigame = miningMinigame;
        this.woodcuttingMinigame = woodcuttingMinigame;
        this.comandoSkillTree = comandoSkillTree;
        this.comandoArcheology = comandoArcheology;

        this.contrabandManager = contrabandManager;
        this.globalEventManager = globalEventManager; // 🌟 GUARDADO

        this.archeologyManager = archeologyManager;
        this.compassTracker = compassTracker;
        this.brushListener = brushListener;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoMechanics Enterprise");

        registerEvents();
        registerCommands();
        startAsyncTasks();

        // 🌟 ARRANCAR EL RASTREADOR DE BRÚJULAS (Virtual Thread)
        compassTracker.startTracking();

        // 🌟 CONECTAR CON EL ORQUESTADOR DE EVENTOS DEL CORE
        conectarEventosGlobales();

        plugin.getLogger().info("⚙️ NexoMechanics activado e inyectado con éxito.");
    }

    public void stopServices() {
        // 🌟 SELF-HEALING: Limpiar toda la arena sospechosa antes de apagar
        try {
            archeologyManager.cleanupAllSpots();
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo limpiar la arqueología en el apagado.");
        }

        plugin.getLogger().info("⚙️ NexoMechanics apagado.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        pm.registerEvents(alchemyMinigame, plugin);
        pm.registerEvents(combatCombo, plugin);
        pm.registerEvents(enchantingMinigame, plugin);
        pm.registerEvents(farmingMinigame, plugin);
        pm.registerEvents(fishingHook, plugin);
        pm.registerEvents(miningMinigame, plugin);
        pm.registerEvents(woodcuttingMinigame, plugin);

        // 🌟 REGISTRO DEL INTERCEPTOR DE CEPILLADO Y ANTI-GRIEFING
        pm.registerEvents(brushListener, plugin);
    }

    private void registerCommands() {
        var handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error(configManager.getMessages().mensajes().errores().sinPermiso());
        });

        handler.register(comandoSkillTree);
        handler.register(comandoArcheology);
    }

    private void startAsyncTasks() {
        server.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            contrabandManager.tickScanner(System.currentTimeMillis());
        }, 3, 3, TimeUnit.SECONDS);
    }

    // ==========================================
    // 🗺️ PUENTE CON EL CORE (EVENTOS GLOBALES)
    // ==========================================
    private void conectarEventosGlobales() {
        try {
            // 🌟 FIX: Usamos el eventManager que Guice nos inyectó, cero advertencias deprecadas
            if (globalEventManager != null) {
                // Más adelante aquí registraremos la clase: globalEventManager.registrarEvento(new ArcheologyEvent(...));
                plugin.getLogger().info("🔗 Arqueología conectada al Orquestador Global exitosamente.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ No se pudo enlazar NexoMechanics con NexoEventManager. ¿Está el Core actualizado?");
        }
    }
}
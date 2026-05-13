package me.nexo.mechanics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.events.NexoEventManager; // 🌟 IMPORT DEL ORQUESTADOR
import me.nexo.mechanics.archeology.ArcheologyManager;
import me.nexo.mechanics.archeology.ArcheologyCompassTracker;
import me.nexo.mechanics.archeology.ArcheologyListener;
import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.commands.ComandoArcheology;
import me.nexo.mechanics.commands.ComandoMechanics; // 🌟 AÑADIDO: Import del comando de reload
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.managers.ContrabandManager;
import me.nexo.mechanics.minigames.*;

// 🌟 IMPORTACIONES DE NEXO GATHERING (Fases 2, 4 y Config)
import me.nexo.mechanics.gathering.world.RegenEngine;
import me.nexo.mechanics.gathering.world.ZoneManager;
import me.nexo.mechanics.gathering.progression.GatheringProfileManager;
import me.nexo.mechanics.gathering.config.GatheringConfigLoader; // 🌟 CARGADOR DE YAML

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

    // 🌟 COMANDOS
    private final ComandoSkillTree comandoSkillTree;
    private final ComandoArcheology comandoArcheology;
    private final ComandoMechanics comandoMechanics; // 🌟 AÑADIDO

    private final ContrabandManager contrabandManager;

    // 🌟 SISTEMAS GLOBALES (INDUCIDOS POR EL CORE VÍA GUICE)
    private final NexoEventManager globalEventManager;

    // 🌟 SISTEMA DE ARQUEOLOGÍA
    private final ArcheologyManager archeologyManager;
    private final ArcheologyCompassTracker compassTracker;
    private final ArcheologyListener brushListener;

    // 🌟 SISTEMA DE RECOLECCIÓN (NEXO GATHERING)
    private final RegenEngine regenEngine;
    private final ZoneManager zoneManager;
    private final GatheringProfileManager profileManager;
    private final GatheringConfigLoader gatheringConfigLoader;

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
                              ComandoMechanics comandoMechanics, // 🌟 INYECTADO
                              ContrabandManager contrabandManager,
                              NexoEventManager globalEventManager,
                              ArcheologyManager archeologyManager,
                              ArcheologyCompassTracker compassTracker,
                              ArcheologyListener brushListener,
                              RegenEngine regenEngine,
                              ZoneManager zoneManager,
                              GatheringProfileManager profileManager,
                              GatheringConfigLoader gatheringConfigLoader)
    {
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
        this.comandoMechanics = comandoMechanics; // 🌟 GUARDADO

        this.contrabandManager = contrabandManager;
        this.globalEventManager = globalEventManager;

        this.archeologyManager = archeologyManager;
        this.compassTracker = compassTracker;
        this.brushListener = brushListener;

        this.regenEngine = regenEngine;
        this.zoneManager = zoneManager;
        this.profileManager = profileManager;
        this.gatheringConfigLoader = gatheringConfigLoader;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoMechanics Enterprise");

        // 🌟 NEXO GATHERING: Cargar zonas desde el config.yml antes de registrar eventos
        gatheringConfigLoader.loadZones();

        registerEvents();
        registerCommands();
        startAsyncTasks();

        compassTracker.startTracking();
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

        // 🌟 NEXO GATHERING: Forzar guardado de seguridad de bloques rotos
        try {
            plugin.getLogger().info("💾 Guardando bloques de Gathering pendientes...");
            regenEngine.forceBackupNow();
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo guardar el backup de Gathering: " + e.getMessage());
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
        pm.registerEvents(brushListener, plugin);

        // 🌟 REGISTRO DEL MOTOR DE ZONAS Y PERFILES (NEXO GATHERING)
        pm.registerEvents(zoneManager, plugin);
        pm.registerEvents(profileManager, plugin);
    }

    private void registerCommands() {
        var handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error(configManager.getMessages().mensajes().errores().sinPermiso());
        });

        handler.register(comandoSkillTree);
        handler.register(comandoArcheology);
        handler.register(comandoMechanics); // 🌟 REGISTRADO
    }

    private void startAsyncTasks() {
        server.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            contrabandManager.tickScanner(System.currentTimeMillis());
        }, 3, 3, TimeUnit.SECONDS);
    }

    private void conectarEventosGlobales() {
        try {
            if (globalEventManager != null) {
                plugin.getLogger().info("🔗 Arqueología conectada al Orquestador Global exitosamente.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ No se pudo enlazar NexoMechanics con NexoEventManager. ¿Está el Core actualizado?");
        }
    }
}
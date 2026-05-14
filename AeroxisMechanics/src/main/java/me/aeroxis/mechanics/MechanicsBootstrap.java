package me.aeroxis.mechanics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.events.AeroxisEventManager;
import me.aeroxis.mechanics.archeology.ArcheologyManager;
import me.aeroxis.mechanics.archeology.ArcheologyCompassTracker;
import me.aeroxis.mechanics.archeology.ArcheologyListener;
import me.aeroxis.mechanics.commands.ComandoSkillTree;
import me.aeroxis.mechanics.commands.ComandoArcheology;
import me.aeroxis.mechanics.commands.ComandoMechanics; // 🌟 AÑADIDO: Import del comando de reload
import me.aeroxis.mechanics.config.ConfigManager;
import me.aeroxis.mechanics.managers.ContrabandManager;
import me.aeroxis.mechanics.minigames.*;
import me.aeroxis.mechanics.minigames.*;

// 🌟 IMPORTACIONES DE NEXO GATHERING (Fases 2, 4 y Config)
import me.aeroxis.mechanics.gathering.world.RegenEngine;
import me.aeroxis.mechanics.gathering.world.ZoneManager;
import me.aeroxis.mechanics.gathering.progression.GatheringProfileManager;
import me.aeroxis.mechanics.gathering.config.GatheringConfigLoader;

// 🌟 IMPORTACIONES DE LATE-GAME (Fase 3)
import me.aeroxis.mechanics.lategame.chronodome.CropMutationEngine;
import me.aeroxis.mechanics.lategame.fracture.MomentumTracker;

import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.concurrent.TimeUnit;

@Singleton
public class MechanicsBootstrap {

    private final AeroxisMechanics plugin;
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
    private final ComandoMechanics comandoMechanics;

    private final ContrabandManager contrabandManager;

    // 🌟 SISTEMAS GLOBALES (INDUCIDOS POR EL CORE VÍA GUICE)
    private final AeroxisEventManager globalEventManager;

    // 🌟 SISTEMA DE ARQUEOLOGÍA
    private final ArcheologyManager archeologyManager;
    private final ArcheologyCompassTracker compassTracker;
    private final ArcheologyListener brushListener;

    // 🌟 SISTEMA DE RECOLECCIÓN (NEXO GATHERING)
    private final RegenEngine regenEngine;
    private final ZoneManager zoneManager;
    private final GatheringProfileManager profileManager;
    private final GatheringConfigLoader gatheringConfigLoader;

    // 🌟 SISTEMAS DE LATE-GAME
    private final CropMutationEngine cropMutationEngine;
    private final MomentumTracker momentumTracker;

    @Inject
    public MechanicsBootstrap(AeroxisMechanics plugin, ConfigManager configManager,
                              AlchemyMinigameManager alchemyMinigame,
                              CombatComboManager combatCombo,
                              EnchantingMinigameManager enchantingMinigame,
                              FarmingMinigameManager farmingMinigame,
                              FishingHookManager fishingHook,
                              MiningMinigameManager miningMinigame,
                              WoodcuttingMinigameManager woodcuttingMinigame,
                              ComandoSkillTree comandoSkillTree,
                              ComandoArcheology comandoArcheology,
                              ComandoMechanics comandoMechanics,
                              ContrabandManager contrabandManager,
                              AeroxisEventManager globalEventManager,
                              ArcheologyManager archeologyManager,
                              ArcheologyCompassTracker compassTracker,
                              ArcheologyListener brushListener,
                              RegenEngine regenEngine,
                              ZoneManager zoneManager,
                              GatheringProfileManager profileManager,
                              GatheringConfigLoader gatheringConfigLoader,
                              CropMutationEngine cropMutationEngine, // 🌟 INYECTADO
                              MomentumTracker momentumTracker)       // 🌟 INYECTADO
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
        this.comandoMechanics = comandoMechanics;

        this.contrabandManager = contrabandManager;
        this.globalEventManager = globalEventManager;

        this.archeologyManager = archeologyManager;
        this.compassTracker = compassTracker;
        this.brushListener = brushListener;

        this.regenEngine = regenEngine;
        this.zoneManager = zoneManager;
        this.profileManager = profileManager;
        this.gatheringConfigLoader = gatheringConfigLoader;

        this.cropMutationEngine = cropMutationEngine; // 🌟 GUARDADO
        this.momentumTracker = momentumTracker;       // 🌟 GUARDADO
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura AeroxisMechanics Enterprise");

        // 🌟 NEXO GATHERING: Cargar zonas desde el config.yml antes de registrar eventos
        gatheringConfigLoader.loadZones();

        registerEvents();
        registerCommands();
        startAsyncTasks();

        compassTracker.startTracking();
        conectarEventosGlobales();

        plugin.getLogger().info("⚙️ AeroxisMechanics activado e inyectado con éxito.");
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

        plugin.getLogger().info("⚙️ AeroxisMechanics apagado.");
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

        // 🌟 REGISTRO DE MECÁNICAS LATE-GAME
        pm.registerEvents(cropMutationEngine, plugin);
        pm.registerEvents(momentumTracker, plugin);
    }

    private void registerCommands() {
        var handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error(configManager.getMessages().mensajes().errores().sinPermiso());
        });

        handler.register(comandoSkillTree);
        handler.register(comandoArcheology);
        handler.register(comandoMechanics);
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
            plugin.getLogger().warning("⚠️ No se pudo enlazar AeroxisMechanics con AeroxisEventManager. ¿Está el Core actualizado?");
        }
    }
}
package me.nexo.mechanics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.config.ConfigManager;
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
 * Rendimiento: Inyección Explícita (Fail-Fast), Cero Service Locators y Registro Nativo.
 */
@Singleton
public class MechanicsBootstrap {

    private final NexoMechanics plugin;
    private final Server server;
    private final ConfigManager configManager;

    // 🌟 INYECCIÓN EXPLÍCITA: Declaramos todas las dependencias para garantizar
    // que Guice valide su existencia en el mismo instante de arrancar (Fail-Fast).
    private final AlchemyMinigameManager alchemyMinigame;
    private final CombatComboManager combatCombo;
    private final EnchantingMinigameManager enchantingMinigame;
    private final FarmingMinigameManager farmingMinigame;
    private final FishingHookManager fishingHook;
    private final MiningMinigameManager miningMinigame;
    private final WoodcuttingMinigameManager woodcuttingMinigame;
    private final ComandoSkillTree comandoSkillTree;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public MechanicsBootstrap(NexoMechanics plugin, ConfigManager configManager,
                              AlchemyMinigameManager alchemyMinigame,
                              CombatComboManager combatCombo,
                              EnchantingMinigameManager enchantingMinigame,
                              FarmingMinigameManager farmingMinigame,
                              FishingHookManager fishingHook,
                              MiningMinigameManager miningMinigame,
                              WoodcuttingMinigameManager woodcuttingMinigame,
                              ComandoSkillTree comandoSkillTree) {
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

        // 🌟 FIX: Registro directo de las instancias inyectadas
        pm.registerEvents(alchemyMinigame, plugin);
        pm.registerEvents(combatCombo, plugin);
        pm.registerEvents(enchantingMinigame, plugin);
        pm.registerEvents(farmingMinigame, plugin);
        pm.registerEvents(fishingHook, plugin);
        pm.registerEvents(miningMinigame, plugin);
        pm.registerEvents(woodcuttingMinigame, plugin);
    }

    private void registerCommands() {
        // Revxrsal BukkitCommandHandler ya maneja su inyección nativa en el CommandMap
        var handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            // 🌟 FIX: Consumo del ConfigManager inyectado
            actor.error(configManager.getMessages().mensajes().errores().sinPermiso());
        });

        handler.register(comandoSkillTree);
    }
}
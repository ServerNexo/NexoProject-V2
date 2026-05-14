package me.nexo.mechanics.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.nexo.mechanics.gathering.hazards.HazardDispatcher;
import me.nexo.mechanics.gathering.hazards.TelegraphEngine;
import me.nexo.mechanics.gathering.progression.GatheringProfileManager;
import me.nexo.mechanics.gathering.progression.MirageBarrierEngine;
import org.bukkit.Bukkit; // 🌟 AÑADIDO: Importamos Bukkit para el getPlugin() seguro

import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.MechanicsBootstrap;
import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.commands.ComandoMechanics; // 🌟 AÑADIDO: Importamos el nuevo comando
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.minigames.AlchemyMinigameManager;
import me.nexo.mechanics.minigames.CombatComboManager;
import me.nexo.mechanics.minigames.EnchantingMinigameManager;
import me.nexo.mechanics.minigames.FarmingMinigameManager;
import me.nexo.mechanics.minigames.FishingHookManager;
import me.nexo.mechanics.minigames.MiningMinigameManager;
import me.nexo.mechanics.minigames.WoodcuttingMinigameManager;

// 🌟 IMPORTACIONES DE NEXO GATHERING (EARLY-GAME)
import me.nexo.mechanics.gathering.managers.SanctuaryManager;
import me.nexo.mechanics.gathering.world.RegenEngine;
import me.nexo.mechanics.gathering.world.ZoneManager;
import me.nexo.mechanics.gathering.config.GatheringConfigLoader;
import me.nexo.mechanics.gathering.data.GatheringRepository;

// 🌟 IMPORTACIONES DE NEXO LATE-GAME (FASE 3)
import me.nexo.mechanics.lategame.yggdrasil.WindPhysicsEngine;
import me.nexo.mechanics.lategame.chronodome.CropMutationEngine;
import me.nexo.mechanics.lategame.fracture.MomentumTracker;
import me.nexo.mechanics.lategame.fracture.AsteroidCollapseEngine; // 🌟 AÑADIDO: Destructor de mundos

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.managers.ClaimManager;

/**
 * 💉 NexoMechanics - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) para prevenir Lag Spikes.
 * Nota: UserManager, CrossplayUtils y dependencias globales ya se heredan del Inyector Padre.
 */
public class MechanicsModule extends AbstractModule {

    private final NexoMechanics plugin;

    public MechanicsModule(NexoMechanics plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia del plugin
        bind(NexoMechanics.class).toInstance(plugin);

        // ==========================================
        // 🚀 ORQUESTADOR
        // ==========================================
        bind(MechanicsBootstrap.class).asEagerSingleton();

        // ==========================================
        // 📂 CONFIGURACIONES
        // ==========================================
        bind(ConfigManager.class).asEagerSingleton();

        // ==========================================
        // 🎮 MINIJUEGOS Y MECÁNICAS (LISTENERS)
        // ==========================================
        bind(AlchemyMinigameManager.class).asEagerSingleton();
        bind(CombatComboManager.class).asEagerSingleton();
        bind(EnchantingMinigameManager.class).asEagerSingleton();
        bind(FarmingMinigameManager.class).asEagerSingleton();
        bind(FishingHookManager.class).asEagerSingleton();
        bind(MiningMinigameManager.class).asEagerSingleton();
        bind(WoodcuttingMinigameManager.class).asEagerSingleton();
        bind(HazardDispatcher.class).asEagerSingleton();
        bind(TelegraphEngine.class).asEagerSingleton();

        // ==========================================
        // ⛰️ NEXO GATHERING (Zonas, Regeneración y Santuarios)
        // ==========================================
        bind(SanctuaryManager.class).asEagerSingleton();
        bind(RegenEngine.class).asEagerSingleton();
        bind(ZoneManager.class).asEagerSingleton();
        bind(GatheringRepository.class).asEagerSingleton();
        bind(GatheringProfileManager.class).asEagerSingleton();
        bind(MirageBarrierEngine.class).asEagerSingleton();
        bind(GatheringConfigLoader.class).asEagerSingleton();

        // ==========================================
        // 🌌 NEXO LATE-GAME (Motores Colosales)
        // ==========================================
        bind(WindPhysicsEngine.class).asEagerSingleton();
        bind(CropMutationEngine.class).asEagerSingleton();
        bind(MomentumTracker.class).asEagerSingleton();
        bind(AsteroidCollapseEngine.class).asEagerSingleton(); // 🌟 AÑADIDO

        // ==========================================
        // ⌨️ COMANDOS (REVXRSAL LAMP)
        // ==========================================
        bind(ComandoSkillTree.class).asEagerSingleton();
        bind(ComandoMechanics.class).asEagerSingleton();
    }

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    /**
     * Puente hacia la Economía: Previene el "Plugin already initialized!"
     */
    @Provides
    @Singleton
    public EconomyManager proveerEconomyManager() {
        NexoEconomy ecoPlugin = (NexoEconomy) Bukkit.getPluginManager().getPlugin("NexoEconomy");
        if (ecoPlugin != null && ecoPlugin.getInjector() != null) {
            return ecoPlugin.getInjector().getInstance(EconomyManager.class);
        }
        return null;
    }

    /**
     * Puente hacia las Protecciones: Previene el "Plugin already initialized!"
     */
    @Provides
    @Singleton
    public ClaimManager proveerClaimManager() {
        NexoProtections protPlugin = (NexoProtections) Bukkit.getPluginManager().getPlugin("NexoProtections");
        if (protPlugin != null && protPlugin.getInjector() != null) {
            return protPlugin.getInjector().getInstance(ClaimManager.class);
        }
        return null;
    }
}
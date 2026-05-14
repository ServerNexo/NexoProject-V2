package me.aeroxis.mechanics.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.economy.AeroxisEconomy;
import me.aeroxis.mechanics.AeroxisMechanics;
import me.aeroxis.mechanics.gathering.hazards.HazardDispatcher;
import me.aeroxis.mechanics.gathering.hazards.TelegraphEngine;
import me.aeroxis.mechanics.gathering.progression.GatheringProfileManager;
import me.aeroxis.mechanics.gathering.progression.MirageBarrierEngine;
import me.aeroxis.protections.AeroxisProtections;
import org.bukkit.Bukkit; // 🌟 AÑADIDO: Importamos Bukkit para el getPlugin() seguro

import me.aeroxis.mechanics.MechanicsBootstrap;
import me.aeroxis.mechanics.commands.ComandoSkillTree;
import me.aeroxis.mechanics.commands.ComandoMechanics; // 🌟 AÑADIDO: Importamos el nuevo comando
import me.aeroxis.mechanics.config.ConfigManager;
import me.aeroxis.mechanics.minigames.AlchemyMinigameManager;
import me.aeroxis.mechanics.minigames.CombatComboManager;
import me.aeroxis.mechanics.minigames.EnchantingMinigameManager;
import me.aeroxis.mechanics.minigames.FarmingMinigameManager;
import me.aeroxis.mechanics.minigames.FishingHookManager;
import me.aeroxis.mechanics.minigames.MiningMinigameManager;
import me.aeroxis.mechanics.minigames.WoodcuttingMinigameManager;

// 🌟 IMPORTACIONES DE NEXO GATHERING (EARLY-GAME)
import me.aeroxis.mechanics.gathering.managers.SanctuaryManager;
import me.aeroxis.mechanics.gathering.world.RegenEngine;
import me.aeroxis.mechanics.gathering.world.ZoneManager;
import me.aeroxis.mechanics.gathering.config.GatheringConfigLoader;
import me.aeroxis.mechanics.gathering.data.GatheringRepository;

// 🌟 IMPORTACIONES DE NEXO LATE-GAME (FASE 3)
import me.aeroxis.mechanics.lategame.yggdrasil.WindPhysicsEngine;
import me.aeroxis.mechanics.lategame.chronodome.CropMutationEngine;
import me.aeroxis.mechanics.lategame.fracture.MomentumTracker;
import me.aeroxis.mechanics.lategame.fracture.AsteroidCollapseEngine; // 🌟 AÑADIDO: Destructor de mundos

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.aeroxis.economy.core.EconomyManager;
import me.aeroxis.protections.managers.ClaimManager;

/**
 * 💉 AeroxisMechanics - Módulo de Inyección de Dependencias (Child Module)
 * Rendimiento: Carga Eager (Instantánea) para prevenir Lag Spikes.
 * Nota: UserManager, CrossplayUtils y dependencias globales ya se heredan del Inyector Padre.
 */
public class MechanicsModule extends AbstractModule {

    private final AeroxisMechanics plugin;

    public MechanicsModule(AeroxisMechanics plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Enlazamos la instancia del plugin
        bind(AeroxisMechanics.class).toInstance(plugin);

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
        AeroxisEconomy ecoPlugin = (AeroxisEconomy) Bukkit.getPluginManager().getPlugin("AeroxisEconomy");
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
        AeroxisProtections protPlugin = (AeroxisProtections) Bukkit.getPluginManager().getPlugin("AeroxisProtections");
        if (protPlugin != null && protPlugin.getInjector() != null) {
            return protPlugin.getInjector().getInstance(ClaimManager.class);
        }
        return null;
    }
}
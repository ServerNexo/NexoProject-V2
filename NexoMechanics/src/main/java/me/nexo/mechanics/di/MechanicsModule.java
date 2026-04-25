package me.nexo.mechanics.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.MechanicsBootstrap;
import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.minigames.AlchemyMinigameManager;
import me.nexo.mechanics.minigames.CombatComboManager;
import me.nexo.mechanics.minigames.EnchantingMinigameManager;
import me.nexo.mechanics.minigames.FarmingMinigameManager;
import me.nexo.mechanics.minigames.FishingHookManager;
import me.nexo.mechanics.minigames.MiningMinigameManager;
import me.nexo.mechanics.minigames.WoodcuttingMinigameManager;

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

        // ==========================================
        // ⌨️ COMANDOS (REVXRSAL LAMP)
        // ==========================================
        bind(ComandoSkillTree.class).asEagerSingleton();
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
        NexoEconomy ecoPlugin = JavaPlugin.getPlugin(NexoEconomy.class);
        return ecoPlugin.getChildInjector().getInstance(EconomyManager.class);
    }

    /**
     * Puente hacia las Protecciones: Previene el "Plugin already initialized!"
     */
    @Provides
    @Singleton
    public ClaimManager proveerClaimManager() {
        NexoProtections protPlugin = JavaPlugin.getPlugin(NexoProtections.class);
        return protPlugin.getChildInjector().getInstance(ClaimManager.class);
    }
}
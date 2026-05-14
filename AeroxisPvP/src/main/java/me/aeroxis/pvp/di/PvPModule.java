package me.aeroxis.pvp.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.pvp.AeroxisPvP;
import org.bukkit.Bukkit; // 🌟 AÑADIDO: Importación segura de Bukkit

import me.aeroxis.pvp.api.PvPBootstrap;
import me.aeroxis.pvp.classes.ArmorClassListener;
import me.aeroxis.pvp.classes.ArmorWeightManager;
import me.aeroxis.pvp.combat.CombatClickListener;
import me.aeroxis.pvp.combat.ComboCacheManager;
import me.aeroxis.pvp.combat.PoiseManager; // 🌟 NUEVA DEPENDENCIA
import me.aeroxis.pvp.config.ConfigManager;
import me.aeroxis.pvp.pasivas.PasivasManager;
import me.aeroxis.pvp.pvp.PvPManager;

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.aeroxis.economy.AeroxisEconomy;
import me.aeroxis.economy.core.EconomyManager;
import me.aeroxis.protections.AeroxisProtections;
import me.aeroxis.protections.managers.ClaimManager;

/**
 * 💉 AeroxisPvP - Módulo de Inyección de Dependencias (Arquitectura Enterprise)
 * Configura los enlaces específicos del submódulo de combate.
 * Nota: Los servicios del Core (Repositorios, Economía, Utils) se heredan automáticamente del CoreInjector.
 */
public class PvPModule extends AbstractModule {

    private final AeroxisPvP plugin;

    public PvPModule(AeroxisPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // 🛡️ Vinculamos la instancia actual del plugin de PvP
        bind(AeroxisPvP.class).toInstance(plugin);

        // ⚔️ REGISTRO DE COMPONENTES DE COMBATE Y PASIVAS
        // Al usar 'asEagerSingleton', Guice los instancia al arrancar el plugin
        // evitando tirones de lag (lazy-loading) durante el primer golpe de una pelea.
        bind(ConfigManager.class).asEagerSingleton();
        bind(PvPManager.class).asEagerSingleton();
        bind(PasivasManager.class).asEagerSingleton();

        // 🌟 SISTEMA DE CLASES (TRINIDAD RPG)
        bind(ArmorWeightManager.class).asEagerSingleton();
        bind(ArmorClassListener.class).asEagerSingleton();

        // ⚔️ SISTEMA DE COMBOS TÁCTICOS Y POSTURA
        bind(PoiseManager.class).asEagerSingleton(); // 🌟 INYECTADO
        bind(ComboCacheManager.class).asEagerSingleton();
        bind(CombatClickListener.class).asEagerSingleton();

        // 🚀 Orquestador principal
        bind(PvPBootstrap.class).asEagerSingleton();
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
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el método estandarizado getInjector()
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
        // 🌟 FIX: Usamos Bukkit.getPluginManager() y el método estandarizado getInjector()
        AeroxisProtections protPlugin = (AeroxisProtections) Bukkit.getPluginManager().getPlugin("AeroxisProtections");
        if (protPlugin != null && protPlugin.getInjector() != null) {
            return protPlugin.getInjector().getInstance(ClaimManager.class);
        }
        return null;
    }
}
package me.aeroxis.war.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.aeroxis.economy.AeroxisEconomy;
import me.aeroxis.war.AeroxisWar;
import org.bukkit.Bukkit; // 🌟 AÑADIDO: Importación segura de Bukkit

import me.aeroxis.war.managers.WarManager;
import me.aeroxis.war.config.ConfigManager;

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.aeroxis.clans.AeroxisClans;
import me.aeroxis.clans.core.ClanManager;
import me.aeroxis.economy.core.EconomyManager;
import me.aeroxis.protections.AeroxisProtections;
import me.aeroxis.protections.managers.ClaimManager;

/**
 * 💉 AeroxisWar - Módulo de Inyección de Dependencias (Arquitectura Enterprise)
 * Configura los enlaces específicos del submódulo de guerra.
 * Nota: UserManager, DatabaseManager y AeroxisCore se heredan automáticamente del CoreInjector.
 */
public class WarModule extends AbstractModule {

    private final AeroxisWar plugin;

    public WarModule(AeroxisWar plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // 🛡️ Vinculamos la instancia actual del plugin de guerra
        bind(AeroxisWar.class).toInstance(plugin);

        // ⚔️ REGISTRO DE COMPONENTES TÁCTICOS
        // Al usar 'asEagerSingleton', Guice los instancia al arrancar el plugin
        // evitando retrasos (lag) cuando un jugador use un comando por primera vez.
        bind(WarManager.class).asEagerSingleton();
        bind(ConfigManager.class).asEagerSingleton();
    }

    // ==========================================
    // 🌉 PUENTES HORIZONTALES (Arquitectura Multi-Módulo)
    // ==========================================

    /**
     * Puente hacia los Clanes: Permite gestionar guerras entre bandos
     */
    @Provides
    @Singleton
    public ClanManager proveerClanManager() {
        // 🌟 FIX: Bukkit.getPluginManager() + getInjector()
        AeroxisClans clansPlugin = (AeroxisClans) Bukkit.getPluginManager().getPlugin("AeroxisClans");
        if (clansPlugin != null && clansPlugin.getInjector() != null) {
            return clansPlugin.getInjector().getInstance(ClanManager.class);
        }
        return null;
    }

    /**
     * Puente hacia la Economía: Permite cobrar la declaración de guerra y dar botines
     */
    @Provides
    @Singleton
    public EconomyManager proveerEconomyManager() {
        // 🌟 FIX: Bukkit.getPluginManager() + getInjector()
        AeroxisEconomy ecoPlugin = (AeroxisEconomy) Bukkit.getPluginManager().getPlugin("AeroxisEconomy");
        if (ecoPlugin != null && ecoPlugin.getInjector() != null) {
            return ecoPlugin.getInjector().getInstance(EconomyManager.class);
        }
        return null;
    }

    /**
     * Puente hacia las Protecciones: Permite el asedio y destrucción controlada
     */
    @Provides
    @Singleton
    public ClaimManager proveerClaimManager() {
        // 🌟 FIX: Bukkit.getPluginManager() + getInjector()
        AeroxisProtections protPlugin = (AeroxisProtections) Bukkit.getPluginManager().getPlugin("AeroxisProtections");
        if (protPlugin != null && protPlugin.getInjector() != null) {
            return protPlugin.getInjector().getInstance(ClaimManager.class);
        }
        return null;
    }
}
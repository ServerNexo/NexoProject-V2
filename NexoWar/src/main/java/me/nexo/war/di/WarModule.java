package me.nexo.war.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import me.nexo.war.NexoWar;
import me.nexo.war.managers.WarManager;
import me.nexo.war.config.ConfigManager;

// 🌟 IMPORTACIONES DE LOS PUENTES HORIZONTALES
import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanManager;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.managers.ClaimManager;

/**
 * 💉 NexoWar - Módulo de Inyección de Dependencias (Arquitectura Enterprise)
 * Configura los enlaces específicos del submódulo de guerra.
 * Nota: UserManager, DatabaseManager y NexoCore se heredan automáticamente del CoreInjector.
 */
public class WarModule extends AbstractModule {

    private final NexoWar plugin;

    public WarModule(NexoWar plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // 🛡️ Vinculamos la instancia actual del plugin de guerra
        bind(NexoWar.class).toInstance(plugin);

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
        NexoClans clansPlugin = JavaPlugin.getPlugin(NexoClans.class);
        return clansPlugin.getChildInjector().getInstance(ClanManager.class);
    }

    /**
     * Puente hacia la Economía: Permite cobrar la declaración de guerra y dar botines
     */
    @Provides
    @Singleton
    public EconomyManager proveerEconomyManager() {
        NexoEconomy ecoPlugin = JavaPlugin.getPlugin(NexoEconomy.class);
        return ecoPlugin.getChildInjector().getInstance(EconomyManager.class);
    }

    /**
     * Puente hacia las Protecciones: Permite el asedio y destrucción controlada
     */
    @Provides
    @Singleton
    public ClaimManager proveerClaimManager() {
        NexoProtections protPlugin = JavaPlugin.getPlugin(NexoProtections.class);
        return protPlugin.getChildInjector().getInstance(ClaimManager.class);
    }
}
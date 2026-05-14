package me.aeroxis.economy;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.economy.bazar.BazaarChatListener;
import me.aeroxis.economy.bazar.BazaarManager;
import me.aeroxis.economy.blackmarket.BlackMarketManager;
import me.aeroxis.economy.commands.ComandoBazar;
import me.aeroxis.economy.commands.ComandoEco;
import me.aeroxis.economy.commands.ComandoMercadoNegro;
import me.aeroxis.economy.commands.ComandoTrade;
import me.nexo.economy.commands.*;
import me.aeroxis.economy.config.ConfigManager;
import me.aeroxis.economy.core.EconomyManager;
import me.aeroxis.economy.core.AeroxisEconomyExpansion; // 🌟 IMPORTAMOS LA EXPANSIÓN DE PAPI
import me.aeroxis.economy.di.EconomyModule;
import me.aeroxis.economy.listeners.EconomyListener;
import me.aeroxis.economy.listeners.TradeListener;
import me.aeroxis.economy.trade.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 💰 AeroxisEconomy - Main Plugin Class (Arquitectura Enterprise)
 * Rendimiento: Child Injector, Inyección Nativa en CommandMap y Cero Estáticos.
 */
public class AeroxisEconomy extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales del Core
    private Injector childInjector;

    private EconomyManager economyManager;
    private TradeManager tradeManager;
    private BazaarManager bazaarManager;
    private BlackMarketManager blackMarketManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("💰 Iniciando AeroxisEconomy (Motor Financiero Seguro)...");

        // 🛡️ Verificación estricta de la dependencia Core
        var corePlugin = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: Falta AeroxisCore. Apagando módulo económico por seguridad.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 💉 INICIALIZACIÓN DE GUICE: Creamos el inyector hijo
        this.childInjector = corePlugin.getInjector().createChildInjector(new EconomyModule(this));

        // 🌟 OBTENEMOS LAS INSTANCIAS DESDE GUICE
        this.configManager = childInjector.getInstance(ConfigManager.class);
        this.economyManager = childInjector.getInstance(EconomyManager.class);
        this.tradeManager = childInjector.getInstance(TradeManager.class);
        this.bazaarManager = childInjector.getInstance(BazaarManager.class);
        this.blackMarketManager = childInjector.getInstance(BlackMarketManager.class);

        // 🌟 REGISTRO DE PLACEHOLDER API
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AeroxisEconomyExpansion(this.economyManager).register();
            getLogger().info("✅ [AeroxisEconomy] PlaceholderAPI detectado. ¡Variables registradas!");
        } else {
            getLogger().warning("⚠️ [AeroxisEconomy] PlaceholderAPI NO ENCONTRADO. El dinero no saldrá en el TAB.");
        }

        // 🌟 Registramos Eventos usando las instancias inyectadas
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(EconomyListener.class), this);
        pm.registerEvents(childInjector.getInstance(TradeListener.class), this);
        pm.registerEvents(childInjector.getInstance(BazaarChatListener.class), this);

        // 🌟 REGISTRO NATIVO DE COMANDOS (PAPER 1.21.5 FIX)
        var commandMap = getServer().getCommandMap();
        commandMap.register("nexoeconomy", childInjector.getInstance(ComandoEco.class));
        commandMap.register("nexoeconomy", childInjector.getInstance(ComandoTrade.class));
        commandMap.register("nexoeconomy", childInjector.getInstance(ComandoBazar.class));
        commandMap.register("nexoeconomy", childInjector.getInstance(ComandoMercadoNegro.class));

        getLogger().info("✅ AeroxisEconomy cargado. El mercado global está en línea.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("💰 Apagando AeroxisEconomy... Sincronizando cuentas y mercado.");

        // 🛡️ GUARDADO SEGURO (Mantenemos la ejecución síncrona aquí para evitar Rollbacks)
        if (economyManager != null) {
            economyManager.saveAllAccountsSync();
        }
        if (bazaarManager != null) {
            bazaarManager.saveMarketSync();
        }

        // Ya no es necesario desregistrar de AeroxisAPI porque el Garbage Collector de Guice limpiará todo automáticamente
        getLogger().info("✅ AeroxisEconomy ha sido deshabilitado de forma segura.");
    }

    // 🌟 FIX DEFINITIVO: Renombramos a getInjector() para estandarizar el ecosistema y evitar crasheos en NexoMechanics
    public Injector getInjector() {
        return childInjector;
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS (PUENTE LEGACY)
    // Guice ya gestiona la caché O(1) internamente. Usar @Inject en constructores.
    // ==========================================

    @Deprecated
    public EconomyManager getEconomyManager() { return economyManager; }
    @Deprecated
    public TradeManager getTradeManager() { return tradeManager; }
    @Deprecated
    public BazaarManager getBazaarManager() { return bazaarManager; }
    @Deprecated
    public BlackMarketManager getBlackMarketManager() { return blackMarketManager; }
    @Deprecated
    public ConfigManager getConfigManager() { return configManager; }
}
package me.nexo.economy;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.economy.bazar.BazaarChatListener;
import me.nexo.economy.bazar.BazaarManager;
import me.nexo.economy.blackmarket.BlackMarketManager;
import me.nexo.economy.commands.*;
import me.nexo.economy.config.ConfigManager;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.di.EconomyModule;
import me.nexo.economy.listeners.EconomyListener;
import me.nexo.economy.listeners.TradeListener;
import me.nexo.economy.trade.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 💰 NexoEconomy - Main Plugin Class (Arquitectura Enterprise)
 * Rendimiento: Child Injector, Inyección Nativa en CommandMap y Cero Estáticos.
 */
public class NexoEconomy extends JavaPlugin {

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
        getLogger().info("💰 Iniciando NexoEconomy (Motor Financiero Seguro)...");

        // 🛡️ Verificación estricta de la dependencia Core
        var corePlugin = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (corePlugin == null) {
            getLogger().severe("❌ Error: Falta NexoCore. Apagando módulo económico por seguridad.");
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

        getLogger().info("✅ NexoEconomy cargado. El mercado global está en línea.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("💰 Apagando NexoEconomy... Sincronizando cuentas y mercado.");

        // 🛡️ GUARDADO SEGURO (Mantenemos la ejecución síncrona aquí para evitar Rollbacks)
        if (economyManager != null) {
            economyManager.saveAllAccountsSync();
        }
        if (bazaarManager != null) {
            bazaarManager.saveMarketSync();
        }

        // Ya no es necesario desregistrar de NexoAPI porque el Garbage Collector de Guice limpiará todo automáticamente
        getLogger().info("✅ NexoEconomy ha sido deshabilitado de forma segura.");
    }

    public Injector getChildInjector() {
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
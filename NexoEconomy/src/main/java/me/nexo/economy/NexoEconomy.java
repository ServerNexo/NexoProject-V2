package me.nexo.economy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.user.NexoAPI;
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
 */
public class NexoEconomy extends JavaPlugin {

    private Injector injector;
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
        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ Error: Falta NexoCore. Apagando módulo económico por seguridad.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 INICIALIZACIÓN DE GUICE (El corazón de la Arquitectura)
        this.injector = Guice.createInjector(new EconomyModule(this));

        // 🌟 OBTENEMOS LAS INSTANCIAS INYECTADAS (Cero "new")
        this.configManager = injector.getInstance(ConfigManager.class);
        this.economyManager = injector.getInstance(EconomyManager.class);
        this.tradeManager = injector.getInstance(TradeManager.class);
        this.bazaarManager = injector.getInstance(BazaarManager.class);
        this.blackMarketManager = injector.getInstance(BlackMarketManager.class);

        // 🌟 Registrar en la API central para que otros módulos lo usen
        NexoAPI.getServices().register(EconomyManager.class, this.economyManager);
        NexoAPI.getServices().register(BazaarManager.class, this.bazaarManager);

        // 🌟 Registramos Eventos usando las instancias inyectadas
        getServer().getPluginManager().registerEvents(injector.getInstance(EconomyListener.class), this);
        getServer().getPluginManager().registerEvents(injector.getInstance(TradeListener.class), this);
        getServer().getPluginManager().registerEvents(injector.getInstance(BazaarChatListener.class), this);

        // 🌟 Registramos Comandos usando Guice
        if (getCommand("eco") != null) {
            getCommand("eco").setExecutor(injector.getInstance(ComandoEco.class));
            getCommand("eco").setTabCompleter(new ComandoEcoTabCompleter());
        }
        if (getCommand("trade") != null) getCommand("trade").setExecutor(injector.getInstance(ComandoTrade.class));
        if (getCommand("bazar") != null) getCommand("bazar").setExecutor(injector.getInstance(ComandoBazar.class));
        if (getCommand("mercadonegro") != null) getCommand("mercadonegro").setExecutor(injector.getInstance(ComandoMercadoNegro.class));

        getLogger().info("✅ NexoEconomy cargado. El mercado global está en línea.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("💰 Apagando NexoEconomy... Sincronizando cuentas y mercado.");

        // 🛡️ GUARDADO SEGURO (Evita rollbacks)
        if (economyManager != null) {
            // Asumiendo que crearás este método de guardado en tu EconomyManager
            economyManager.saveAllAccountsSync();
        }
        if (bazaarManager != null) {
            // Asumiendo que crearás este método de guardado en tu BazaarManager
            bazaarManager.saveMarketSync();
        }

        NexoAPI.getServices().unregister(EconomyManager.class);
        NexoAPI.getServices().unregister(BazaarManager.class);
        getLogger().info("NexoEconomy ha sido deshabilitado de forma segura.");
    }

    // Getters para compatibilidad con clases antiguas que aún no migramos
    public EconomyManager getEconomyManager() { return economyManager; }
    public TradeManager getTradeManager() { return tradeManager; }
    public BazaarManager getBazaarManager() { return bazaarManager; }
    public BlackMarketManager getBlackMarketManager() { return blackMarketManager; }
    public ConfigManager getConfigManager() { return configManager; }
}
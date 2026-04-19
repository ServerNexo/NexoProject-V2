package me.nexo.factories;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.user.NexoAPI;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.config.ConfigManager;
import me.nexo.factories.di.FactoriesModule;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * 🏭 NexoFactories - Main Plugin Class (Arquitectura NATIVA)
 */
public class NexoFactories extends JavaPlugin {

    private Injector injector;
    private ConfigManager configManager;
    private FactoryManager factoryManager;
    private BlueprintManager blueprintManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏭 Iniciando NexoFactories (Motor Industrial Zero-Lag)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null ||
                getServer().getPluginManager().getPlugin("NexoProtections") == null) {
            getLogger().severe("❌ Error: Faltan dependencias (NexoCore o NexoProtections).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 INICIALIZACIÓN DE GUICE (El corazón de la Arquitectura)
        this.injector = Guice.createInjector(new FactoriesModule(this));

        // 🌟 OBTENEMOS LAS INSTANCIAS DESDE GUICE
        this.configManager = injector.getInstance(ConfigManager.class);
        this.factoryManager = injector.getInstance(FactoryManager.class);
        this.blueprintManager = injector.getInstance(BlueprintManager.class);

        // Registramos en el API central
        NexoAPI.getServices().register(FactoryManager.class, this.factoryManager);

        factoryManager.loadFactoriesAsync().thenRun(() -> {
            getLogger().info("✅ ¡Fábricas cargadas asíncronamente!");
            getServer().getScheduler().runTaskTimer(this, factoryManager::tickFactories, 20L * 60, 20L * 60);
        });

        // 🌟 Registramos Eventos usando las instancias inyectadas
        getServer().getPluginManager().registerEvents(blueprintManager, this);
        getServer().getPluginManager().registerEvents(injector.getInstance(FactoryInteractListener.class), this);

        // 🌟 FIX: INYECCIÓN DE COMANDOS NATIVOS POR REFLEXIÓN
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

            // 💉 Inyectamos el comando saltándonos la seguridad de Paper
            commandMap.register("nexofactories", injector.getInstance(ComandoFactory.class));

            getLogger().info("✅ Comando de Fábricas inyectado nativamente (Zero-Lag).");
        } catch (Exception e) {
            getLogger().severe("❌ Error inyectando comando de NexoFactories: " + e.getMessage());
            e.printStackTrace();
        }

        getLogger().info("✅ ¡NexoFactories cargado! Nexo-Grid en línea y produciendo.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏭 Apagando NexoFactories... Guardando datos en la nube.");
        if (factoryManager != null) {
            factoryManager.saveAllFactoriesSync();
        }
        NexoAPI.getServices().unregister(FactoryManager.class);
        getLogger().info("NexoFactories ha sido deshabilitado.");
    }

    public FactoryManager getFactoryManager() { return factoryManager; }
    public BlueprintManager getBlueprintManager() { return blueprintManager; }
    public ConfigManager getConfigManager() { return configManager; }
}
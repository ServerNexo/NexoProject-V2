package me.nexo.factories;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.di.FactoriesModule;
import me.nexo.factories.listeners.CraftingStationListener;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.BlueprintScanner;
import me.nexo.factories.managers.FactoryManager;
import me.nexo.factories.managers.RecipeManager; // 🌟 IMPORTAMOS EL GESTOR DE RECETAS
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

/**
 * 🏭 NexoFactories - Main Plugin Class (Arquitectura Enterprise Java 21)
 * Rendimiento: Child Injector, Folia Async Scheduler e Inyección Pura.
 */
public class NexoFactories extends JavaPlugin {

    private Injector childInjector;
    private FactoryManager factoryManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🏭 Iniciando NexoFactories (Motor Industrial)...");

        // 🌟 1. ADAPTACIÓN SKYBLOCK
        var core = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (core == null) {
            getLogger().severe("❌ Error crítico: Falta la dependencia NexoCore.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 2. CREACIÓN DEL INYECTOR HIJO
        this.childInjector = core.getInjector().createChildInjector(new FactoriesModule(this));

        // 🌟 3. OBTENCIÓN DE INSTANCIAS VÍA GUICE
        this.factoryManager = childInjector.getInstance(FactoryManager.class);
        var blueprintManager = childInjector.getInstance(BlueprintManager.class);
        var factoryInteractListener = childInjector.getInstance(FactoryInteractListener.class);
        var blueprintScanner = childInjector.getInstance(BlueprintScanner.class);
        var craftingStationListener = childInjector.getInstance(CraftingStationListener.class);
        var comandoFactory = childInjector.getInstance(ComandoFactory.class);

        // 🌟 DESPERTAMOS AL GESTOR DE RECETAS (Lee el YAML al iniciar)
        var recipeManager = childInjector.getInstance(RecipeManager.class);

        // 🌟 4. CARGA ASÍNCRONA Y SCHEDULER DE PAPER/FOLIA
        factoryManager.loadFactoriesAsync().thenRun(() -> {
            getLogger().info("✅ ¡Fábricas cargadas asíncronamente!");
            getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
                factoryManager.tickFactories();
            }, 5, 5, TimeUnit.SECONDS);
        });

        // 🌟 5. REGISTRO DE EVENTOS
        var pm = getServer().getPluginManager();
        pm.registerEvents(blueprintManager, this);
        pm.registerEvents(factoryInteractListener, this);
        pm.registerEvents(blueprintScanner, this);
        pm.registerEvents(craftingStationListener, this);

        // 🌟 6. REGISTRO NATIVO DE COMANDOS
        try {
            var commandMap = getServer().getCommandMap();
            commandMap.register("nexofactories", comandoFactory);
            getLogger().info("✅ Comandos de fábrica inyectados exitosamente.");
        } catch (Exception e) {
            getLogger().severe("❌ Error al inyectar comandos: " + e.getMessage());
        }

        getLogger().info("✅ ¡NexoFactories en línea!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏭 Apagando NexoFactories... Guardando datos industriales.");
        if (factoryManager != null) {
            factoryManager.saveAllFactoriesSync();
        }
    }

    public Injector getInjector() {
        return childInjector;
    }
}
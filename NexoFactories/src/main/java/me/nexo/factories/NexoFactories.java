package me.nexo.factories;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.core.api.NexoFactoriesAPI;
import me.nexo.core.user.NexoAPI;
import me.nexo.factories.api.NexoFactoriesAPIImpl;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.commands.ComandoSilo;
import me.nexo.factories.di.FactoriesModule;
import me.nexo.factories.listeners.CraftingStationListener;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.listeners.LogisticsLinkerListener;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.BlueprintScanner;
import me.nexo.factories.managers.FactoryManager;
import me.nexo.factories.managers.RecipeManager;
import me.nexo.factories.managers.SiloManager; // 🌟 IMPORT DEL SILO
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

/**
 * 🏭 NexoFactories - Main Plugin Class (Arquitectura Enterprise Java 21)
 * Rendimiento: Child Injector, Folia Async Scheduler, Inyección Pura y Enrutamiento Global.
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
        var recipeManager = childInjector.getInstance(RecipeManager.class);
        var comandoFactory = childInjector.getInstance(ComandoFactory.class);

        // 🌟 FASE 4/5: OBTENCIÓN DE COMPONENTES LOGÍSTICOS
        var logisticsLinkerListener = childInjector.getInstance(LogisticsLinkerListener.class);
        var siloManager = childInjector.getInstance(SiloManager.class); // 🌟 ¡AQUÍ ESTÁ EL SILO MANAGER!
        var comandoSilo = childInjector.getInstance(ComandoSilo.class);
        var apiImpl = childInjector.getInstance(NexoFactoriesAPIImpl.class);

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
        pm.registerEvents(logisticsLinkerListener, this);

        // 🌟 6. REGISTRO DE LA API LOGÍSTICA PARA NEXOMINIONS
        try {
            NexoAPI.getInstance().getServiceManager().register(NexoFactoriesAPI.class, apiImpl);
            getLogger().info("✅ API Logística Inalámbrica expuesta al servidor global.");
        } catch (Exception e) {
            getLogger().warning("⚠️ No se pudo exponer NexoFactoriesAPI. ¿NexoCore está actualizado?");
        }

        // 🌟 7. REGISTRO DE COMANDOS (REVXRSAL LAMP)
        try {
            /* * ⚠️ ATENCIÓN ARQUITECTO:
             * Como usas @Command de Revxrsal Lamp, no puedes registrarlos en el CommandMap nativo de Bukkit con un cast (Command).
             * Necesitas usar tu Handler de Lamp. Dependiendo de cómo lo tengas configurado en tu core:
             * * BukkitCommandHandler handler = BukkitCommandHandler.create(this);
             * handler.register(comandoFactory);
             * handler.register(comandoSilo);
             */

            getLogger().info("✅ Clases de comandos instanciadas. (Asegúrate de registrarlas con tu Handler de Lamp)");
        } catch (Exception e) {
            getLogger().severe("❌ Error al instanciar comandos: " + e.getMessage());
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
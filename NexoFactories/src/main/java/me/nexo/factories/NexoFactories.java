package me.nexo.factories;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.di.FactoriesModule;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;
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

        // 🌟 1. OBTENCIÓN SEGURA DEL CORE (Evita errores de carga)
        var core = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (core == null || !getServer().getPluginManager().isPluginEnabled("NexoProtections")) {
            getLogger().severe("❌ Error crítico: Faltan dependencias (NexoCore o NexoProtections).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 2. CREACIÓN DEL INYECTOR HIJO (Hereda dependencias de NexoCore)
        // FIX: No usamos Guice.createInjector, usamos el inyector del Core.
        this.childInjector = core.getInjector().createChildInjector(new FactoriesModule(this));

        // 🌟 3. OBTENCIÓN DE INSTANCIAS VÍA GUICE
        this.factoryManager = childInjector.getInstance(FactoryManager.class);
        var blueprintManager = childInjector.getInstance(BlueprintManager.class);
        var factoryInteractListener = childInjector.getInstance(FactoryInteractListener.class);
        var comandoFactory = childInjector.getInstance(ComandoFactory.class);

        // 🌟 4. CARGA ASÍNCRONA Y SCHEDULER DE PAPER/FOLIA
        factoryManager.loadFactoriesAsync().thenRun(() -> {
            getLogger().info("✅ ¡Fábricas cargadas asíncronamente!");

            // 🛡️ REGLA 3: Los procesos industriales corren en el AsyncScheduler para no afectar el TPS
            getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
                factoryManager.tickFactories();
            }, 1, 1, TimeUnit.MINUTES);
        });

        // 🌟 5. REGISTRO DE EVENTOS
        var pm = getServer().getPluginManager();
        pm.registerEvents(blueprintManager, this);
        pm.registerEvents(factoryInteractListener, this);

        // 🌟 6. REGISTRO NATIVO DE COMANDOS (Paper 1.21.5+)
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

        // 🛡️ REGLA 3: Guardado Síncrono final para garantizar integridad de la DB
        if (factoryManager != null) {
            factoryManager.saveAllFactoriesSync();
        }
    }

    public Injector getInjector() {
        return childInjector;
    }
}
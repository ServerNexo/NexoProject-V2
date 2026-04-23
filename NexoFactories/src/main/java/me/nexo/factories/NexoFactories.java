package me.nexo.factories;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.factories.commands.ComandoFactory;
import me.nexo.factories.di.FactoriesModule;
import me.nexo.factories.listeners.FactoryInteractListener;
import me.nexo.factories.managers.BlueprintManager;
import me.nexo.factories.managers.FactoryManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

/**
 * 🏭 NexoFactories - Main Plugin Class (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Async Scheduler, Inyección Pura y Registro de CommandMap Nativo.
 */
public class NexoFactories extends JavaPlugin {

    private Injector injector;
    private FactoryManager factoryManager;

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

        // 🌟 OBTENEMOS LAS INSTANCIAS BASE
        this.factoryManager = injector.getInstance(FactoryManager.class);
        var blueprintManager = injector.getInstance(BlueprintManager.class);
        var factoryInteractListener = injector.getInstance(FactoryInteractListener.class);
        var comandoFactory = injector.getInstance(ComandoFactory.class);

        // 🌟 CARGA ASÍNCRONA DE DATOS Y SCHEDULER DE FOLIA
        factoryManager.loadFactoriesAsync().thenRun(() -> {
            getLogger().info("✅ ¡Fábricas cargadas asíncronamente desde la Base de Datos!");
            
            // 🛡️ PAPER/FOLIA ASYNC SCHEDULER: Se ejecuta cada 1 minuto (Reemplaza los viejos 1200 Ticks)
            getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
                factoryManager.tickFactories();
            }, 1, 1, TimeUnit.MINUTES);
        });

        // 🌟 REGISTRO DE EVENTOS (Instancias Inyectadas)
        getServer().getPluginManager().registerEvents(blueprintManager, this);
        getServer().getPluginManager().registerEvents(factoryInteractListener, this);

        // 🌟 REGISTRO NATIVO DE COMANDOS (CommandMap Paper 1.21.5+)
        try {
            var commandMap = getServer().getCommandMap();
            commandMap.register("nexofactories", comandoFactory);
            getLogger().info("✅ Comandos nativos inyectados exitosamente.");
        } catch (Exception e) {
            getLogger().severe("❌ Error al inyectar comandos en el CommandMap: " + e.getMessage());
        }

        getLogger().info("✅ ¡NexoFactories cargado! Nexo-Grid en línea y produciendo.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("🏭 Apagando NexoFactories... Guardando datos industriales.");
        
        // 🛡️ LÓGICA SÍNCRONA DE APAGADO: Obligatorio para evitar pérdida de datos (Regla 3)
        if (factoryManager != null) {
            factoryManager.saveAllFactoriesSync();
        }
        
        getLogger().info("NexoFactories ha sido deshabilitado de forma segura.");
    }
}
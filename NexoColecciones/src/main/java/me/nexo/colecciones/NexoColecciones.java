package me.nexo.colecciones;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.colecciones.api.ColeccionesExpansion;
import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.FlushTask;
import me.nexo.colecciones.commands.ComandoColecciones;
import me.nexo.colecciones.commands.ComandoColeccionesTabCompleter;
import me.nexo.colecciones.commands.ComandoSlayer;
import me.nexo.colecciones.config.ConfigManager;
import me.nexo.colecciones.di.ColeccionesModule;
import me.nexo.colecciones.slayers.SlayerListener;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.user.NexoAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 📚 NexoColecciones - Clase Principal (Arquitectura Enterprise)
 */
public class NexoColecciones extends JavaPlugin {

    private Injector injector;

    private ConfigManager configManager;
    private ColeccionesConfig coleccionesConfig;
    private CollectionManager collectionManager;
    private SlayerManager slayerManager;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("📚 Iniciando NexoColecciones (Motor Enterprise)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ FATAL: NexoCore no encontrado. El sistema de colecciones se apagará.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 1. INICIALIZAMOS GUICE (Inyección de Dependencias)
        this.injector = Guice.createInjector(new ColeccionesModule(this));

        // 🌟 2. OBTENEMOS LAS INSTANCIAS COMPARTIDAS DESDE GUICE
        this.configManager = injector.getInstance(ConfigManager.class);
        this.coleccionesConfig = injector.getInstance(ColeccionesConfig.class);
        this.collectionManager = injector.getInstance(CollectionManager.class);
        this.slayerManager = injector.getInstance(SlayerManager.class);

        // 🌟 3. REGISTRAMOS LOS SERVICIOS EN LA API GLOBAL
        NexoAPI.getServices().register(CollectionManager.class, this.collectionManager);
        NexoAPI.getServices().register(SlayerManager.class, this.slayerManager);

        // 🌟 4. CARGAMOS LOS DATOS Y CONFIGURACIONES
        this.coleccionesConfig.recargarConfig();
        this.collectionManager.cargarDesdeConfig();
        this.slayerManager.cargarSlayers();

        // 🌟 5. REGISTRAMOS LOS EVENTOS INYECTADOS
        getServer().getPluginManager().registerEvents(injector.getInstance(ColeccionesListener.class), this);
        getServer().getPluginManager().registerEvents(injector.getInstance(SlayerListener.class), this);

        // 🌟 6. REGISTRAMOS COMANDOS
        if (getCommand("colecciones") != null) {
            getCommand("colecciones").setExecutor(injector.getInstance(ComandoColecciones.class));
            getCommand("colecciones").setTabCompleter(new ComandoColeccionesTabCompleter()); // Inmutable, no ocupa inyección
        }
        if (getCommand("slayer") != null) {
            getCommand("slayer").setExecutor(injector.getInstance(ComandoSlayer.class));
        }

        // 🌟 7. EXPANSIÓN PAPI
        // 🌟 7. EXPANSIÓN PAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // 🌟 FIX: Le pasamos el this.collectionManager que nos pide el constructor
            new ColeccionesExpansion(this, this.collectionManager).register();
        }

        // 🌟 8. INICIAR TAREA DE AUTO-GUARDADO ASÍNCRONA
        // Ejecutamos la tarea inyectada cada 5 minutos (6000 ticks) usando el Scheduler del Servidor
        // 🌟 FIX: Envolvemos la tarea en un Runnable (Lambda) y ejecutamos su método .run()
        // 🌟 8. INICIAR TAREA DE AUTO-GUARDADO ASÍNCRONA
        // Ejecutamos la tarea inyectada que ya trae su propio motor
        injector.getInstance(FlushTask.class).start();

        getLogger().info("✅ NexoColecciones habilitado y conectado a NexoCore.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("📚 Guardando progreso de los jugadores...");

        // 🌟 GUARDADO SÍNCRONO DE EMERGENCIA: Extraemos la ÚNICA instancia y la forzamos a guardar
        if (injector != null) {
            try {
                injector.getInstance(FlushTask.class).forceFlushSync();
            } catch (Exception e) {
                getLogger().severe("❌ Error forzando el guardado final: " + e.getMessage());
            }
        }

        NexoAPI.getServices().unregister(CollectionManager.class);
        NexoAPI.getServices().unregister(SlayerManager.class);
        getLogger().info("✅ NexoColecciones apagado de forma segura.");
    }

    // 🌟 GETTERS (Mantenidos temporalmente hasta migrar todos los menús)
    public ConfigManager getConfigManager() { return configManager; }
    public CollectionManager getCollectionManager() { return collectionManager; }
    public ColeccionesConfig getColeccionesConfig() { return coleccionesConfig; }
    public SlayerManager getSlayerManager() { return slayerManager; }
}
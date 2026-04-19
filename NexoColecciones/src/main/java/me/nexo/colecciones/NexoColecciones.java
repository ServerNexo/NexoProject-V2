package me.nexo.colecciones;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.colecciones.api.ColeccionesExpansion;
import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.FlushTask;
import me.nexo.colecciones.commands.ComandoColecciones;
import me.nexo.colecciones.commands.ComandoSlayer;
import me.nexo.colecciones.config.ConfigManager;
import me.nexo.colecciones.di.ColeccionesModule;
import me.nexo.colecciones.slayers.SlayerListener;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.user.NexoAPI;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * 📚 NexoColecciones - Clase Principal (Arquitectura NATIVA Bypassed)
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

        // 🌟 FIX: 6. INYECCIÓN DE COMANDOS NATIVOS POR REFLEXIÓN
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

            commandMap.register("nexocolecciones", injector.getInstance(ComandoColecciones.class));
            commandMap.register("nexocolecciones", injector.getInstance(ComandoSlayer.class));

            getLogger().info("✅ Comandos de Colecciones inyectados nativamente (Zero-Lag).");
        } catch (Exception e) {
            getLogger().severe("❌ Error inyectando comandos de Colecciones: " + e.getMessage());
            e.printStackTrace();
        }

        // 🌟 7. EXPANSIÓN PAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ColeccionesExpansion(this, this.collectionManager).register();
        }

        // 🌟 8. INICIAR TAREA DE AUTO-GUARDADO ASÍNCRONA
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
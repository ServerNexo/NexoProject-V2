package me.nexo.colecciones;

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
import me.nexo.core.NexoCore;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * 📚 NexoColecciones - Clase Principal (Arquitectura Enterprise)
 * Rendimiento: Child Injector, Cero Service Locators y Comandos Inyectados Nativamente.
 */
public class NexoColecciones extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("📚 Iniciando NexoColecciones (Motor Enterprise)...");

        // 🌟 1. OBTENEMOS EL INYECTOR MAESTRO DEL CORE Y CREAMOS EL HIJO
        this.childInjector = NexoCore.getInstance().getInjector().createChildInjector(new ColeccionesModule(this));

        // 🌟 2. CARGAMOS CONFIGURACIONES Y DATOS (A través de Guice)
        childInjector.getInstance(ColeccionesConfig.class).recargarConfig();
        childInjector.getInstance(CollectionManager.class).cargarDesdeConfig();
        childInjector.getInstance(SlayerManager.class).cargarSlayers();

        // 🌟 3. REGISTRAMOS EVENTOS INYECTADOS
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(ColeccionesListener.class), this);
        pm.registerEvents(childInjector.getInstance(SlayerListener.class), this);

        // 🌟 4. INYECCIÓN NATIVA DE COMANDOS (CommandMap vía Reflexión)
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

            // Nota: Asumimos que ComandoColecciones y ComandoSlayer ya extienden de org.bukkit.command.Command
            commandMap.register("nexocolecciones", childInjector.getInstance(ComandoColecciones.class));
            commandMap.register("nexocolecciones", childInjector.getInstance(ComandoSlayer.class));
        } catch (Exception e) {
            getLogger().severe("❌ Error inyectando comandos al CommandMap: " + e.getMessage());
        }

        // 🌟 5. EXPANSIÓN PAPI (Le pasamos el inyector si necesita acceder a los managers)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ColeccionesExpansion(this, childInjector).register();
        }

        // 🌟 6. INICIAR TAREA DE AUTO-GUARDADO ASÍNCRONA
        childInjector.getInstance(FlushTask.class).runTaskTimerAsynchronously(this, 6000L, 6000L);

        getLogger().info("✅ NexoColecciones habilitado y conectado a NexoCore.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("📚 Guardando progreso de los jugadores...");

        // 🌟 GUARDADO SÍNCRONO DE EMERGENCIA: Extraemos la ÚNICA instancia y la forzamos a guardar
        if (childInjector != null) {
            try {
                childInjector.getInstance(FlushTask.class).forceFlushSync();
            } catch (Exception e) {
                getLogger().severe("❌ Error forzando el guardado final: " + e.getMessage());
            }
        }

        getLogger().info("✅ NexoColecciones apagado de forma segura.");
    }

    public Injector getChildInjector() {
        return childInjector;
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS (PUENTE LEGACY)
    // ==========================================
    
    @Deprecated
    public ConfigManager getConfigManager() { return childInjector.getInstance(ConfigManager.class); }
    
    @Deprecated
    public CollectionManager getCollectionManager() { return childInjector.getInstance(CollectionManager.class); }
    
    @Deprecated
    public ColeccionesConfig getColeccionesConfig() { return childInjector.getInstance(ColeccionesConfig.class); }
    
    @Deprecated
    public SlayerManager getSlayerManager() { return childInjector.getInstance(SlayerManager.class); }
}
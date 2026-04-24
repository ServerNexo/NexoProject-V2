package me.nexo.colecciones;

import com.google.inject.Injector;
import me.nexo.colecciones.api.ColeccionesExpansion;
import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.ColeccionesListener;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.colecciones.FlushTask;
import me.nexo.colecciones.commands.ComandoColecciones;
import me.nexo.colecciones.commands.ComandoSlayer;
import me.nexo.colecciones.di.ColeccionesModule;
import me.nexo.colecciones.slayers.SlayerListener;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.NexoCore;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 📚 NexoColecciones - Clase Principal (Arquitectura Enterprise Java 21)
 * Rendimiento: FlushTask Nativo, CommandMap Nativo y Cero Service Locators.
 */
public class NexoColecciones extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("📚 Iniciando NexoColecciones (Motor Enterprise)...");

        // 🌟 1. OBTENEMOS EL CORE DE FORMA SEGURA
        var core = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (core == null) {
            getLogger().severe("❌ Error crítico: NexoCore no encontrado. Apagando submódulo.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Creamos el inyector hijo
        this.childInjector = core.getInjector().createChildInjector(new ColeccionesModule(this));

        // 🌟 2. CARGAMOS CONFIGURACIONES Y DATOS (A través de Guice)
        childInjector.getInstance(ColeccionesConfig.class).recargarConfig();
        childInjector.getInstance(CollectionManager.class).cargarDesdeConfig();
        childInjector.getInstance(SlayerManager.class).cargarSlayers();

        // 🌟 3. REGISTRAMOS EVENTOS INYECTADOS
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(ColeccionesListener.class), this);
        pm.registerEvents(childInjector.getInstance(SlayerListener.class), this);

        // 🌟 4. INYECCIÓN NATIVA DE COMANDOS
        try {
            var commandMap = getServer().getCommandMap();
            commandMap.register("nexocolecciones", childInjector.getInstance(ComandoColecciones.class));
            commandMap.register("nexoslayer", childInjector.getInstance(ComandoSlayer.class));
            getLogger().info("✅ Comandos inyectados nativamente en el CommandMap.");
        } catch (Exception e) {
            getLogger().severe("❌ Error inyectando comandos al CommandMap: " + e.getMessage());
        }

        // 🌟 5. EXPANSIÓN PAPI (FIX: Dejamos que Guice inyecte el CollectionManager automáticamente)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            childInjector.getInstance(ColeccionesExpansion.class).register();
        }

        // 🌟 6. AUTO-GUARDADO ASÍNCRONO NATIVO
        // FIX: Tu FlushTask ya tiene su propio ThreadPool interno, solo llamamos a start()
        childInjector.getInstance(FlushTask.class).start();

        getLogger().info("✅ NexoColecciones habilitado y conectado a NexoCore.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("📚 Guardando progreso de los jugadores...");

        // 🌟 GUARDADO SÍNCRONO DE EMERGENCIA
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
}
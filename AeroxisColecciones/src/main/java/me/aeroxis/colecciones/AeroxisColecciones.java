package me.aeroxis.colecciones;

import com.google.inject.Injector;
import me.aeroxis.colecciones.api.ColeccionesExpansion;
import me.aeroxis.colecciones.colecciones.ColeccionesConfig;
import me.aeroxis.colecciones.colecciones.ColeccionesListener;
import me.aeroxis.colecciones.colecciones.CollectionManager;
import me.aeroxis.colecciones.colecciones.FlushTask;
import me.aeroxis.colecciones.commands.ComandoColecciones;
import me.aeroxis.colecciones.commands.ComandoSlayer;
import me.aeroxis.colecciones.di.ColeccionesModule;
import me.aeroxis.colecciones.slayers.SlayerListener;
import me.aeroxis.colecciones.slayers.SlayerManager;
import me.aeroxis.core.AeroxisCore;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 📚 AeroxisColecciones - Clase Principal (Arquitectura Enterprise Java 21)
 * Rendimiento: FlushTask Nativo, CommandMap Nativo y Cero Service Locators.
 */
public class AeroxisColecciones extends JavaPlugin {

    // 🌟 Usamos un Inyector Hijo para heredar dependencias globales (Core)
    private Injector childInjector;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("📚 Iniciando AeroxisColecciones (Motor Enterprise)...");

        // 🌟 1. OBTENEMOS EL CORE DE FORMA SEGURA
        var core = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (core == null) {
            getLogger().severe("❌ Error crítico: AeroxisCore no encontrado. Apagando submódulo.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Creamos el inyector hijo
        this.childInjector = core.getInjector().createChildInjector(new ColeccionesModule(this));

        // 🌟 2. CARGAMOS CONFIGURACIONES Y DATOS (A través de Guice)
        var colConfig = childInjector.getInstance(ColeccionesConfig.class);
        var colManager = childInjector.getInstance(CollectionManager.class);

        colConfig.recargarConfig();

        // 🌟 FIX MÓDULO 1: Cargamos las plantillas de recompensas ANTES que las colecciones
        colManager.loadRewardTemplates(colConfig.getRecompensasConfig());
        colManager.cargarDesdeConfig();

        childInjector.getInstance(SlayerManager.class).cargarSlayers();

        // 🌟 3. REGISTRAMOS EVENTOS INYECTADOS
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(ColeccionesListener.class), this);
        pm.registerEvents(childInjector.getInstance(SlayerListener.class), this);

        // 🌟 4. INYECCIÓN NATIVA DE COMANDOS
        try {
            var commandMap = getServer().getCommandMap();
            commandMap.register("aeroxiscolecciones", childInjector.getInstance(ComandoColecciones.class));
            commandMap.register("aeroxisslayer", childInjector.getInstance(ComandoSlayer.class));
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

        getLogger().info("✅ AeroxisColecciones habilitado y conectado a AeroxisCore.");
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

        getLogger().info("✅ AeroxisColecciones apagado de forma segura.");
    }

    // 🌟 FIX: Renombramos a getInjector() para mantener la arquitectura estandarizada
    public Injector getInjector() {
        return childInjector;
    }
}
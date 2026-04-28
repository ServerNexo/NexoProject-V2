package me.nexo.core.di;

import com.google.inject.AbstractModule;
import me.nexo.core.NexoCore;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import me.nexo.core.api.NexoWebServer;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.api.ServiceBootstrap;
import me.nexo.core.utils.Base64Util;
import me.nexo.core.utils.NexoColor;
import me.nexo.core.NexoPasterService; // 🌟 NUEVO IMPORT DEL MOTOR
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin; // 🌟 IMPORT PARA GUICE

/**
 * 🏛️ Nexo Network - Módulo Principal de Guice (Arquitectura Enterprise)
 * Define cómo se construyen e inyectan las dependencias del Core.
 */
public class NexoCoreModule extends AbstractModule {

    private final NexoCore plugin;

    public NexoCoreModule(NexoCore plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // ==========================================
        // ⚙️ INSTANCIAS NATIVAS DE PAPER API
        // ==========================================
        bind(Plugin.class).toInstance(plugin);
        bind(JavaPlugin.class).toInstance(plugin); // 🌟 NECESARIO PARA EL PASTER
        bind(NexoCore.class).toInstance(plugin);
        bind(Server.class).toInstance(plugin.getServer());

        // ==========================================
        // 🗄️ REPOSITORIOS Y BASES DE DATOS
        // ==========================================
        bind(ConfigManager.class).asEagerSingleton();
        bind(DatabaseManager.class).asEagerSingleton();
        bind(UserRepository.class).asEagerSingleton();
        bind(UserManager.class).asEagerSingleton();

        // ==========================================
        // 🛠️ SERVICIOS DE UTILIDAD Y MOTORES
        // ==========================================
        bind(NexoColor.class).asEagerSingleton();
        bind(Base64Util.class).asEagerSingleton();

        // 🚀 MOTOR DE ESTRUCTURAS (SUSTITUTO DE FAWE)
        bind(NexoPasterService.class).asEagerSingleton();

        // Si ya migraste CrossplayUtils, descomenta la siguiente línea:
        // bind(me.nexo.core.crossplay.CrossplayUtils.class).asEagerSingleton();

        // ==========================================
        // 🌐 API WEB Y ARRANQUE DEL SISTEMA
        // ==========================================
        bind(NexoWebServer.class).asEagerSingleton();
        bind(ServiceBootstrap.class).asEagerSingleton();
    }
}
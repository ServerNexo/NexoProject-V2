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
import me.nexo.core.NexoPasterService;
import me.nexo.core.crossplay.CrossplayUtils;

// 🌟 IMPORTACIONES FASE 2 (HUB Y EVENTOS)
import me.nexo.core.cataclysms.CataclysmManager;
import me.nexo.core.cataclysms.MeteorListener;
import me.nexo.core.hub.HubDonationManager;
import me.nexo.core.hub.HubDonationGUI;

// 🌟 IMPORTACIONES FASE 6 (META-JUEGO SOCIAL)
import me.nexo.core.clans.FanManager;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
        bind(JavaPlugin.class).toInstance(plugin);
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
        // 🛠️ SERVICIOS DE UTILIDAD Y MOTORES GLOBALES
        // ==========================================
        bind(NexoColor.class).asEagerSingleton();
        bind(Base64Util.class).asEagerSingleton();
        bind(CrossplayUtils.class).asEagerSingleton();

        // 🚀 MOTOR DE ESTRUCTURAS (SUSTITUTO DE FAWE)
        bind(NexoPasterService.class).asEagerSingleton();

        // ==========================================
        // 🌍 FASE 2: EVOLUCIÓN DEL ENTORNO
        // ==========================================
        bind(CataclysmManager.class).asEagerSingleton();
        bind(MeteorListener.class).asEagerSingleton();
        bind(HubDonationManager.class).asEagerSingleton();
        bind(HubDonationGUI.class).asEagerSingleton();

        // ==========================================
        // 🚩 FASE 6: META-JUEGO SOCIAL (SISTEMA DE FANS)
        // ==========================================
        bind(FanManager.class).asEagerSingleton();

        // ==========================================
        // 🌐 API WEB Y ARRANQUE DEL SISTEMA
        // ==========================================
        bind(NexoWebServer.class).asEagerSingleton();
        bind(ServiceBootstrap.class).asEagerSingleton();
    }
}
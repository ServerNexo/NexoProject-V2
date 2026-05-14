package me.aeroxis.core.di;

import com.google.inject.AbstractModule;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.bosses.AeroxisBossRegistry;
import me.aeroxis.core.menus.CosmeticsHubRegistry;
import me.aeroxis.core.database.DatabaseManager;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.user.UserRepository;
import me.aeroxis.core.api.AeroxisWebServer;
import me.aeroxis.core.config.ConfigManager;
import me.aeroxis.core.api.ServiceBootstrap;
import me.aeroxis.core.utils.Base64Util;
import me.aeroxis.core.utils.AeroxisColor;
import me.aeroxis.core.AeroxisPasterService;
import me.aeroxis.core.crossplay.CrossplayUtils;

// 🌟 IMPORTACIONES FASE 2 (HUB Y EVENTOS)
import me.aeroxis.core.cataclysms.CataclysmManager;
import me.aeroxis.core.cataclysms.MeteorListener;
import me.aeroxis.core.hub.HubDonationManager;
import me.aeroxis.core.hub.HubDonationGUI;

// 🌟 IMPORTACIONES FASE 6 (META-JUEGO SOCIAL)
import me.aeroxis.core.clans.FanManager;

// 🌟 NUEVAS IMPORTACIONES: MOTOR DE JEFES
import me.aeroxis.core.bosses.GlobalBossCombatListener;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 🏛️ Nexo Network - Módulo Principal de Guice (Arquitectura Enterprise)
 * Define cómo se construyen e inyectan las dependencias del Core.
 */
public class AeroxisCoreModule extends AbstractModule {

    private final AeroxisCore plugin;

    public AeroxisCoreModule(AeroxisCore plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // ==========================================
        // ⚙️ INSTANCIAS NATIVAS DE PAPER API
        // ==========================================
        bind(Plugin.class).toInstance(plugin);
        bind(JavaPlugin.class).toInstance(plugin);
        bind(AeroxisCore.class).toInstance(plugin);
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
        bind(AeroxisColor.class).asEagerSingleton();
        bind(Base64Util.class).asEagerSingleton();
        bind(CrossplayUtils.class).asEagerSingleton();

        // 🚀 MOTOR DE ESTRUCTURAS (SUSTITUTO DE FAWE)
        bind(AeroxisPasterService.class).asEagerSingleton();

        // ==========================================
        // 👑 API DE JEFES GLOBALES (AeroxisBossRegistry)
        // ==========================================
        bind(AeroxisBossRegistry.class).asEagerSingleton();
        bind(GlobalBossCombatListener.class).asEagerSingleton(); // Inyectamos el interceptador de daño

        // ==========================================
        // 🎨 API DE MENÚS DINÁMICOS
        // ==========================================
        bind(CosmeticsHubRegistry.class).asEagerSingleton();

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
        bind(AeroxisWebServer.class).asEagerSingleton();
        bind(ServiceBootstrap.class).asEagerSingleton();
    }
}
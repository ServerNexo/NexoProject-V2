package me.aeroxis.cosmetics;

import com.google.inject.Injector;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.CosmeticsHubRegistry;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.utils.SoundManager;
import me.aeroxis.cosmetics.commands.ComandoCosmetics;
import me.aeroxis.cosmetics.di.CosmeticsModule;
import me.aeroxis.cosmetics.engine.CosmeticEngine;
import me.aeroxis.cosmetics.listeners.CosmeticListener;
import me.aeroxis.cosmetics.listeners.SkyblockCosmeticListener;
import me.aeroxis.cosmetics.manager.BoomboxManager;
import me.aeroxis.cosmetics.manager.CosmeticManager;
import me.aeroxis.cosmetics.menus.BoomboxMenu;
import me.aeroxis.cosmetics.menus.VisualEffectsMenu;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler; // 🌟 IMPORTACIÓN LAMP

import java.io.File;
import java.util.List;

/**
 * ✨ AeroxisCosmetics - Módulo de Monetización y Efectos Visuales (AAA)
 * Rendimiento Extremo: Matemáticas Asíncronas y Entity Schedulers.
 */
public class AeroxisCosmetics extends JavaPlugin {

    private Injector childInjector;
    private BukkitCommandHandler commandHandler; // 🌟 GESTOR LAMP

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("✨ Iniciando AeroxisCosmetics (Motor Folia-Ready)...");

        // ==========================================
        // 🌟 1. FORZAR CREACIÓN DE CARPETAS NATIVAS
        // ==========================================
        saveDefaultConfig(); // Crea plugins/AeroxisCosmetics/config.yml automáticamente

        File songsFolder = new File(getDataFolder(), "songs");
        if (!songsFolder.exists()) {
            songsFolder.mkdirs(); // Crea plugins/AeroxisCosmetics/songs/
            getLogger().info("🎵 Carpeta 'songs' generada exitosamente.");
        }

        // 2. Verificar el Puente (AeroxisCore)
        var core = (AeroxisCore) getServer().getPluginManager().getPlugin("AeroxisCore");
        if (core == null) {
            getLogger().severe("❌ Error: AeroxisCore no está instalado. AeroxisCosmetics se apagará.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Inyección de Dependencias
        this.childInjector = core.getInjector().createChildInjector(new CosmeticsModule(this));

        // 4. Arrancar los Motores Asíncronos (Partículas y Mascotas)
        var engine = childInjector.getInstance(CosmeticEngine.class);
        engine.startEngines();

        // 5. Registro de Eventos
        var pm = getServer().getPluginManager();
        pm.registerEvents(childInjector.getInstance(CosmeticListener.class), this);

        if (pm.getPlugin("AeroxisIslas") != null && pm.getPlugin("AeroxisMinions") != null) {
            pm.registerEvents(childInjector.getInstance(SkyblockCosmeticListener.class), this);
            getLogger().info("🏝️ Integración con AeroxisIslas y AeroxisMinions activada.");
        }

        // ==========================================
        // 🌟 MAGIA MODULAR: REGISTRO DE MENÚS EN EL CORE
        // ==========================================
        var hubRegistry = childInjector.getInstance(CosmeticsHubRegistry.class);
        var crossplayUtils = childInjector.getInstance(CrossplayUtils.class);
        var userManager = childInjector.getInstance(UserManager.class);
        var soundManager = childInjector.getInstance(SoundManager.class);
        var cosmeticManager = childInjector.getInstance(CosmeticManager.class);
        var boomboxManager = childInjector.getInstance(BoomboxManager.class);

        // Botón 1: Efectos Visuales (Slot 12)
        hubRegistry.registerCategory(
                12,
                Material.BLAZE_POWDER,
                "<light_purple><bold>Efectos Visuales</bold></light_purple>",
                List.of("<gray>Alas, estelas de flechas</gray>", "<gray>y efectos de asesinato.</gray>"),
                player -> new VisualEffectsMenu(player, crossplayUtils, userManager, soundManager, cosmeticManager).open()
        );
        getLogger().info("🔗 [AeroxisCosmetics] Botón de Efectos Visuales inyectado en el Core Hub.");

        // Botón 2: Boombox Premium (Slot 14)
        hubRegistry.registerCategory(
                14,
                Material.JUKEBOX,
                "<green><bold>Boombox Premium</bold></green>",
                List.of("<gray>Reproduce pistas musicales</gray>", "<gray>para ti y tus amigos cercanos.</gray>"),
                player -> new BoomboxMenu(player, crossplayUtils, soundManager, boomboxManager).open()
        );
        getLogger().info("🔗 [AeroxisCosmetics] Botón de Boombox inyectado en el Core Hub.");

        // ==========================================
        // 🌟 REGISTRO DE COMANDOS (LAMP)
        // ==========================================
        this.commandHandler = BukkitCommandHandler.create(this);
        this.commandHandler.register(childInjector.getInstance(ComandoCosmetics.class));

        getLogger().info("✅ ¡AeroxisCosmetics en línea y optimizado para Folia!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("✨ Apagando AeroxisCosmetics...");
    }

    public Injector getInjector() {
        return childInjector;
    }
}
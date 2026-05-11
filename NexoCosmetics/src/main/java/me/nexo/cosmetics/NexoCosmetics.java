package me.nexo.cosmetics;

import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.CosmeticsHubRegistry;
import me.nexo.core.user.UserManager;
import me.nexo.core.utils.SoundManager;
import me.nexo.cosmetics.di.CosmeticsModule;
import me.nexo.cosmetics.engine.CosmeticEngine;
import me.nexo.cosmetics.listeners.CosmeticListener;
import me.nexo.cosmetics.listeners.SkyblockCosmeticListener;
import me.nexo.cosmetics.manager.BoomboxManager;
import me.nexo.cosmetics.manager.CosmeticManager;
import me.nexo.cosmetics.menus.BoomboxMenu;
import me.nexo.cosmetics.menus.VisualEffectsMenu;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler; // 🌟 IMPORTACIÓN LAMP

import java.io.File;
import java.util.List;

/**
 * ✨ NexoCosmetics - Módulo de Monetización y Efectos Visuales (AAA)
 * Rendimiento Extremo: Matemáticas Asíncronas y Entity Schedulers.
 */
public class NexoCosmetics extends JavaPlugin {

    private Injector childInjector;
    private BukkitCommandHandler commandHandler; // 🌟 GESTOR LAMP

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("✨ Iniciando NexoCosmetics (Motor Folia-Ready)...");

        // ==========================================
        // 🌟 1. FORZAR CREACIÓN DE CARPETAS NATIVAS
        // ==========================================
        saveDefaultConfig(); // Crea plugins/NexoCosmetics/config.yml automáticamente

        File songsFolder = new File(getDataFolder(), "songs");
        if (!songsFolder.exists()) {
            songsFolder.mkdirs(); // Crea plugins/NexoCosmetics/songs/
            getLogger().info("🎵 Carpeta 'songs' generada exitosamente.");
        }

        // 2. Verificar el Puente (NexoCore)
        var core = (NexoCore) getServer().getPluginManager().getPlugin("NexoCore");
        if (core == null) {
            getLogger().severe("❌ Error: NexoCore no está instalado. NexoCosmetics se apagará.");
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

        if (pm.getPlugin("NexoIslas") != null && pm.getPlugin("NexoMinions") != null) {
            pm.registerEvents(childInjector.getInstance(SkyblockCosmeticListener.class), this);
            getLogger().info("🏝️ Integración con NexoIslas y NexoMinions activada.");
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
        getLogger().info("🔗 [NexoCosmetics] Botón de Efectos Visuales inyectado en el Core Hub.");

        // Botón 2: Boombox Premium (Slot 14)
        hubRegistry.registerCategory(
                14,
                Material.JUKEBOX,
                "<green><bold>Boombox Premium</bold></green>",
                List.of("<gray>Reproduce pistas musicales</gray>", "<gray>para ti y tus amigos cercanos.</gray>"),
                player -> new BoomboxMenu(player, crossplayUtils, soundManager, boomboxManager).open()
        );
        getLogger().info("🔗 [NexoCosmetics] Botón de Boombox inyectado en el Core Hub.");

        // ==========================================
        // 🌟 REGISTRO DE COMANDOS (LAMP)
        // ==========================================
        this.commandHandler = BukkitCommandHandler.create(this);
        this.commandHandler.register(childInjector.getInstance(me.nexo.cosmetics.commands.ComandoCosmetics.class));

        getLogger().info("✅ ¡NexoCosmetics en línea y optimizado para Folia!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("✨ Apagando NexoCosmetics...");
    }

    public Injector getInjector() {
        return childInjector;
    }
}
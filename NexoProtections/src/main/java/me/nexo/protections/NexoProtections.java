package me.nexo.protections;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.nexo.core.NexoCore;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.di.ProtectionsModule;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class NexoProtections extends JavaPlugin {

    private Injector injector;
    private ProtectionsBootstrap bootstrap;

    @Override
    public void onEnable() {
        getLogger().info("========================================");
        getLogger().info("🛡️ Pre-iniciando NexoProtections (Esperando enlace seguro con el Core)...");

        if (getServer().getPluginManager().getPlugin("NexoCore") == null) {
            getLogger().severe("❌ NexoCore no detectado. Apagando módulo de Protecciones por seguridad...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 🌟 EL CANDADO: Esperamos a que NexoCore confirme que la BD y su Guice están listos
        NexoCore.getInstance().getCoreReadyFuture().thenRun(() -> {

            getLogger().info("🔓 Luz verde recibida de NexoCore. Arrancando motor de protecciones...");

            // 💉 Inicializar Inyección de forma segura
            this.injector = Guice.createInjector(new ProtectionsModule(this));

            // 🚀 Arrancar Orquestador
            this.bootstrap = injector.getInstance(ProtectionsBootstrap.class);
            this.bootstrap.startServices();

            // 🌟 FIX: Inyección Nativa por Reflexión
            try {
                Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

                commandMap.register("nexoprotections", injector.getInstance(me.nexo.protections.commands.ComandoProteccion.class));
                getLogger().info("✅ Comandos inyectados nativamente.");
            } catch (Exception e) {
                getLogger().severe("❌ Error inyectando comandos nativos en Protections: " + e.getMessage());
                e.printStackTrace();
            }

            getLogger().info("✅ ¡NexoProtections cargado y operativo!");
            getLogger().info("========================================");

        }).exceptionally(ex -> {
            getLogger().severe("❌ Error fatal esperando al Core en NexoProtections: " + ex.getMessage());
            return null;
        });
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stopServices();
        }
    }

    // 🌟 Comando para recargar (usado desde ComandoProteccion.java)
    public void reloadSystem() {
        if (injector == null) return; // 🛡️ Protección anti-crasheo
        getLogger().info("🔄 Recargando NexoProtections...");

        ClaimManager claimManager = injector.getInstance(ClaimManager.class);
        claimManager.getAllStones().clear();
        claimManager.loadAllStonesAsync();

        // 💡 También recargamos los textos
        injector.getInstance(ConfigManager.class).reloadMessages();
    }

    // ==========================================
    // 💡 GETTERS PARA APIS Y MENÚS EXTERNOS
    // ==========================================
    public ConfigManager getConfigManager() {
        if (injector == null) return null; // 🛡️ Protección anti-NPE
        return injector.getInstance(ConfigManager.class);
    }

    public ClaimManager getClaimManager() {
        if (injector == null) return null; // 🛡️ Protección anti-NPE
        return injector.getInstance(ClaimManager.class);
    }
}
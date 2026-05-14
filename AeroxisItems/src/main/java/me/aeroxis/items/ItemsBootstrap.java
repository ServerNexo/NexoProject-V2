package me.aeroxis.items;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.aeroxis.items.accesorios.AccesoriosListener;
import me.aeroxis.items.artefactos.ArtefactoListener;
import me.aeroxis.items.commands.ComandoUpgrade;
import me.aeroxis.items.config.ConfigManager;
import me.aeroxis.items.estaciones.DesguaceListener;
import me.aeroxis.items.estaciones.HerreriaListener;
import me.aeroxis.items.estaciones.ReforjaListener;
import me.aeroxis.items.estaciones.YunqueListener;
import me.aeroxis.items.mecanicas.*;
import me.aeroxis.items.mochilas.PVMenu;
import me.aeroxis.items.estaciones.*;
import me.aeroxis.items.guardarropa.GuardarropaListener;
import me.aeroxis.items.guardarropa.ComandoWardrobe;
import me.aeroxis.items.mecanicas.*;
import me.aeroxis.items.mochilas.MochilaListener;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ AeroxisItems - Orquestador Enterprise (Java 21)
 * Rendimiento: Cero llamadas estáticas, Sinergia con Child Injectors y Lamp Command Framework.
 */
@Singleton
public class ItemsBootstrap {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final AeroxisItems plugin;
    private final Server server;
    private final Injector injector;

    // Inyectamos las dependencias que requieren ejecución en el apagado (Cero Estáticos)
    private final BlockBreakListener blockBreakListener;
    // Inyectar el ConfigManager para sacar el mensaje de error de Lamp (dependencia transitiva)
    private final ConfigManager configManager;

    @Inject
    public ItemsBootstrap(AeroxisItems plugin, Injector injector, BlockBreakListener blockBreakListener, ConfigManager configManager) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
        this.blockBreakListener = blockBreakListener;
        this.configManager = configManager;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura AeroxisItems Enterprise");

        // 🌟 FIX: Guice ya se encarga de crear el Singleton de ItemManager en el momento en que se inyecta.
        // No necesitamos inicializarlo manualmente ni registrarlo en AeroxisAPI gracias al Child Injector.

        registerEvents();
        registerCommands();

        plugin.getLogger().info("🎒 AeroxisItems activado e inyectado con éxito.");
    }

    public void stopServices() {
        // 🛡️ LÓGICA DE SEGURIDAD PRESERVADA Y AISLADA (Mediante instancia, NO estático)
        blockBreakListener.restaurarTodosLosBloques();

        // 🌟 PAPER NATIVE: Iteración segura de inventarios abiertos para prevenir dupes en el reload/stop
        for (var p : server.getOnlinePlayers()) {
            var topInv = p.getOpenInventory().getTopInventory();
            var holder = topInv.getHolder();

            if (holder instanceof PVMenu || holder instanceof GuardarropaListener) {
                p.closeInventory();
            }
        }

        plugin.getLogger().info("🎒 AeroxisItems apagado de forma segura.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        // 🌟 Registro inyectado y limpio (Garantiza que Guice construya cada clase con sus dependencias)
        pm.registerEvents(injector.getInstance(ArmorListener.class), plugin);
        pm.registerEvents(injector.getInstance(CraftingListener.class), plugin);
        pm.registerEvents(injector.getInstance(DesguaceListener.class), plugin);
        pm.registerEvents(injector.getInstance(HerreriaListener.class), plugin);
        pm.registerEvents(injector.getInstance(ReforjaListener.class), plugin);
        pm.registerEvents(injector.getInstance(YunqueListener.class), plugin);
        pm.registerEvents(injector.getInstance(ItemProtectionListener.class), plugin);
        pm.registerEvents(blockBreakListener, plugin);
        pm.registerEvents(injector.getInstance(FishingListener.class), plugin);
        pm.registerEvents(injector.getInstance(DamageListener.class), plugin);
        pm.registerEvents(injector.getInstance(InteractListener.class), plugin);
        pm.registerEvents(injector.getInstance(PlayerItemListener.class), plugin);
        pm.registerEvents(injector.getInstance(VanillaStationsListener.class), plugin);
        pm.registerEvents(injector.getInstance(AccesoriosListener.class), plugin);
        pm.registerEvents(injector.getInstance(ArtefactoListener.class), plugin);
        pm.registerEvents(injector.getInstance(GuardarropaListener.class), plugin);
        pm.registerEvents(injector.getInstance(MochilaListener.class), plugin);

        // 🌟 NUEVO: Registramos nuestro flamante sistema de Botín de Jefes nativo
        pm.registerEvents(injector.getInstance(BossLootListener.class), plugin);

        // 🌟 El sincronizador fantasma cobra vida y protege los ítems
        pm.registerEvents(injector.getInstance(LazyItemSyncer.class), plugin);
    }

    private void registerCommands() {
        // 🌟 LAMP FRAMEWORK: Permite registro dinámico.
        var handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error(configManager.getMessages().mensajes().errores().sinPermiso());
        });

        // Comandos purificados de Lamp
        handler.register(injector.getInstance(ComandoDesguace.class));
        handler.register(injector.getInstance(ComandoUpgrade.class));

        // 🌟 NUEVO: Comando de Guardarropa (Refactorizado sin herencia de Command)
        handler.register(injector.getInstance(ComandoWardrobe.class));
    }
}
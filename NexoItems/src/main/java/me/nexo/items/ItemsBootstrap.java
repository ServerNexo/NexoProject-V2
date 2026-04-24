package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.items.accesorios.AccesoriosListener;
import me.nexo.items.artefactos.ArtefactoListener;
import me.nexo.items.estaciones.*;
import me.nexo.items.guardarropa.GuardarropaListener;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.mecanicas.*;
import me.nexo.items.mochilas.MochilaListener;
import org.bukkit.Server;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoItems - Orquestador Enterprise (Java 21)
 * Rendimiento: Cero llamadas estáticas, Sinergia con Child Injectors y Lamp Command Framework.
 */
@Singleton
public class ItemsBootstrap {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final Server server;
    private final Injector injector;

    // Inyectamos las dependencias que requieren ejecución en el apagado (Cero Estáticos)
    private final BlockBreakListener blockBreakListener;
    // Inyectar el ConfigManager para sacar el mensaje de error de Lamp (dependencia transitiva)
    private final me.nexo.items.config.ConfigManager configManager;

    @Inject
    public ItemsBootstrap(NexoItems plugin, Injector injector, BlockBreakListener blockBreakListener, me.nexo.items.config.ConfigManager configManager) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
        this.blockBreakListener = blockBreakListener;
        this.configManager = configManager;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoItems Enterprise");

        // 🌟 FIX: Guice ya se encarga de crear el Singleton de ItemManager en el momento en que se inyecta.
        // No necesitamos inicializarlo manualmente ni registrarlo en NexoAPI gracias al Child Injector.

        registerEvents();
        registerCommands();

        plugin.getLogger().info("🎒 NexoItems activado e inyectado con éxito.");
    }

    public void stopServices() {
        // 🛡️ LÓGICA DE SEGURIDAD PRESERVADA Y AISLADA (Mediante instancia, NO estático)
        // 🌟 FIX ERROR MÉTODO: El nombre correcto en el Listener purificado es restaurarTodosLosBloques()
        blockBreakListener.restaurarTodosLosBloques();

        // 🌟 PAPER NATIVE: Iteración segura de inventarios abiertos para prevenir dupes en el reload/stop
        for (var p : server.getOnlinePlayers()) {
            var topInv = p.getOpenInventory().getTopInventory();
            var holder = topInv.getHolder();

            if (holder instanceof me.nexo.items.mochilas.PVMenu || holder instanceof GuardarropaListener) {
                p.closeInventory();
            }
        }

        plugin.getLogger().info("🎒 NexoItems apagado de forma segura.");
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
        pm.registerEvents(blockBreakListener, plugin); // Ya lo tenemos inyectado en el constructor
        pm.registerEvents(injector.getInstance(FishingListener.class), plugin);
        pm.registerEvents(injector.getInstance(DamageListener.class), plugin);
        pm.registerEvents(injector.getInstance(InteractListener.class), plugin);
        pm.registerEvents(injector.getInstance(PlayerItemListener.class), plugin);
        pm.registerEvents(injector.getInstance(VanillaStationsListener.class), plugin);
        pm.registerEvents(injector.getInstance(AccesoriosListener.class), plugin);
        pm.registerEvents(injector.getInstance(ArtefactoListener.class), plugin);
        pm.registerEvents(injector.getInstance(GuardarropaListener.class), plugin);
        pm.registerEvents(injector.getInstance(MochilaListener.class), plugin);

        // 🌟 El sincronizador fantasma cobra vida y protege los ítems
        pm.registerEvents(injector.getInstance(LazyItemSyncer.class), plugin);
    }

    private void registerCommands() {
        // 🌟 LAMP FRAMEWORK: Permite registro dinámico. 
        // Aunque no extiendan org.bukkit.command.Command directamente, Lamp mapea todo al CommandMap de Bukkit/Paper nativamente.
        var handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            // Evitamos NullPointerExceptions usando dependencias inyectadas en lugar de llamadas transitivas largas
            actor.error(configManager.getMessages().mensajes().errores().sinPermiso());
        });

        // Comandos purificados de Lamp
        handler.register(injector.getInstance(me.nexo.items.ComandoDesguace.class));
        handler.register(injector.getInstance(me.nexo.items.commands.ComandoUpgrade.class));
    }
}
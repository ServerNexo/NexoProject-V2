package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.nexo.core.user.NexoAPI;
import me.nexo.items.accesorios.AccesoriosListener;
import me.nexo.items.artefactos.ArtefactoListener;
import me.nexo.items.estaciones.*;
import me.nexo.items.guardarropa.GuardarropaListener;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.mecanicas.*;
import me.nexo.items.mochilas.MochilaListener;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import revxrsal.commands.bukkit.BukkitCommandHandler;

/**
 * 🏛️ NexoItems - Orquestador Enterprise
 */
@Singleton
public class ItemsBootstrap {

    private final NexoItems plugin;
    private final Server server;
    private final Injector injector;

    @Inject
    public ItemsBootstrap(NexoItems plugin, Injector injector) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.injector = injector;
    }

    public void startServices() {
        plugin.getLogger().info("⚡ Arrancando Arquitectura NexoItems Enterprise");

        // Inicializamos los managers estáticos heredados
        ItemManager.init(plugin);
        NexoAPI.getServices().register(ItemManager.class, injector.getInstance(ItemManager.class));

        registerEvents();
        registerCommands();

        plugin.getLogger().info("🎒 NexoItems activado e inyectado con éxito.");
    }

    public void stopServices() {
        // 🛡️ LÓGICA DE SEGURIDAD PRESERVADA Y AISLADA
        BlockBreakListener.restaurarBloquesRotos();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof me.nexo.items.mochilas.PVMenu ||
                    p.getOpenInventory().getTopInventory().getHolder() instanceof GuardarropaListener) {
                p.closeInventory();
            }
        }

        NexoAPI.getServices().unregister(ItemManager.class);
        plugin.getLogger().info("🎒 NexoItems apagado de forma segura.");
    }

    private void registerEvents() {
        var pm = server.getPluginManager();

        // 🌟 Registro inyectado y limpio
        pm.registerEvents(injector.getInstance(ArmorListener.class), plugin);
        pm.registerEvents(injector.getInstance(CraftingListener.class), plugin);
        pm.registerEvents(injector.getInstance(DesguaceListener.class), plugin);
        pm.registerEvents(injector.getInstance(HerreriaListener.class), plugin);
        pm.registerEvents(injector.getInstance(ReforjaListener.class), plugin);
        pm.registerEvents(injector.getInstance(YunqueListener.class), plugin);
        pm.registerEvents(injector.getInstance(ItemProtectionListener.class), plugin);
        pm.registerEvents(injector.getInstance(BlockBreakListener.class), plugin);
        pm.registerEvents(injector.getInstance(FishingListener.class), plugin);
        pm.registerEvents(injector.getInstance(DamageListener.class), plugin);
        pm.registerEvents(injector.getInstance(InteractListener.class), plugin);
        pm.registerEvents(injector.getInstance(PlayerItemListener.class), plugin);
        pm.registerEvents(injector.getInstance(VanillaStationsListener.class), plugin);
        pm.registerEvents(injector.getInstance(AccesoriosListener.class), plugin);
        pm.registerEvents(injector.getInstance(ArtefactoListener.class), plugin);
        pm.registerEvents(injector.getInstance(GuardarropaListener.class), plugin);
        pm.registerEvents(injector.getInstance(MochilaListener.class), plugin);

        // 🌟 AÑADIDO: ¡Ahora el sincronizador fantasma cobra vida y protege los ítems!
        pm.registerEvents(injector.getInstance(LazyItemSyncer.class), plugin);
    }

    private void registerCommands() {
        BukkitCommandHandler handler = BukkitCommandHandler.create(plugin);

        handler.registerExceptionHandler(revxrsal.commands.exception.NoPermissionException.class, (actor, exception) -> {
            actor.error(plugin.getConfigManager().getMessages().mensajes().errores().sinPermiso());
        });

        // 🌟 Aquí registraremos los comandos purificados de Lamp
        handler.register(injector.getInstance(ComandoDesguace.class));
        handler.register(injector.getInstance(me.nexo.items.commands.ComandoUpgrade.class));
    }
}
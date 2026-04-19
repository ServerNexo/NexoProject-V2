package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Injector;
import me.nexo.items.accesorios.AccesoriosListener;
import me.nexo.items.accesorios.ComandoAccesorios;
import me.nexo.items.artefactos.ArtefactoListener;
import me.nexo.items.commands.ComandoUpgrade;
import me.nexo.items.estaciones.DesguaceListener;
import me.nexo.items.estaciones.HerreriaListener;
import me.nexo.items.estaciones.ReforjaListener;
import me.nexo.items.estaciones.YunqueListener;
import me.nexo.items.guardarropa.ComandoWardrobe;
import me.nexo.items.guardarropa.GuardarropaListener;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import me.nexo.items.mecanicas.*;
import me.nexo.items.mochilas.ComandoPV;
import me.nexo.items.mochilas.MochilaListener;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

public class ItemsBootstrap {

    private final NexoItems plugin;
    private final Injector injector;

    @Inject
    public ItemsBootstrap(NexoItems plugin, Injector injector) {
        this.plugin = plugin;
        this.injector = injector;
    }

    public void startServices() {
        // 1. Cargar Archivos
        injector.getInstance(FileManager.class).cargarArchivos();
        ItemManager.init(plugin);

        // 2. Registrar Eventos
        registerEvents();

        // 🌟 FIX: 3. INYECCIÓN DE COMANDOS NATIVOS POR REFLEXIÓN
        registerCommands();
    }

    public void stopServices() {
        // Guardado de mochilas y guardarropas (Se manejan en NexoCore al salir, pero por si acaso)
    }

    private void registerEvents() {
        var pm = plugin.getServer().getPluginManager();

        // Estaciones
        pm.registerEvents(injector.getInstance(YunqueListener.class), plugin);
        pm.registerEvents(injector.getInstance(ReforjaListener.class), plugin);
        pm.registerEvents(injector.getInstance(HerreriaListener.class), plugin);
        pm.registerEvents(injector.getInstance(DesguaceListener.class), plugin);

        // Mecánicas
        pm.registerEvents(injector.getInstance(ArmorListener.class), plugin);
        pm.registerEvents(injector.getInstance(DamageListener.class), plugin);
        pm.registerEvents(injector.getInstance(BlockBreakListener.class), plugin);
        pm.registerEvents(injector.getInstance(FishingListener.class), plugin);
        pm.registerEvents(injector.getInstance(InteractListener.class), plugin);
        pm.registerEvents(injector.getInstance(ItemProtectionListener.class), plugin);
        pm.registerEvents(injector.getInstance(PlayerItemListener.class), plugin);
        pm.registerEvents(injector.getInstance(VanillaStationsListener.class), plugin);
        pm.registerEvents(injector.getInstance(CraftingListener.class), plugin);

        // Sistemas
        pm.registerEvents(injector.getInstance(MochilaListener.class), plugin);
        pm.registerEvents(injector.getInstance(GuardarropaListener.class), plugin);
        pm.registerEvents(injector.getInstance(AccesoriosListener.class), plugin);
        pm.registerEvents(injector.getInstance(ArtefactoListener.class), plugin);
    }

    private void registerCommands() {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            // 💉 Inyectamos los 6 comandos nativamente saltándonos la seguridad de Paper
            commandMap.register("nexoitems", injector.getInstance(ComandoUpgrade.class));
            commandMap.register("nexoitems", injector.getInstance(ComandoAccesorios.class));
            commandMap.register("nexoitems", injector.getInstance(ComandoWardrobe.class));
            commandMap.register("nexoitems", injector.getInstance(ComandoPV.class));
            commandMap.register("nexoitems", injector.getInstance(ComandoDesguace.class));
            commandMap.register("nexoitems", injector.getInstance(ComandoTest.class));

            plugin.getLogger().info("✅ Comandos de NexoItems inyectados nativamente (Zero-Lag).");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error inyectando comandos de NexoItems: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
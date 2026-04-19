package me.nexo.mechanics;

import com.google.inject.Inject;
import com.google.inject.Injector;
import me.nexo.mechanics.commands.ComandoSkillTree;
import me.nexo.mechanics.minigames.*;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

public class MechanicsBootstrap {

    private final NexoMechanics plugin;
    private final Injector injector;

    @Inject
    public MechanicsBootstrap(NexoMechanics plugin, Injector injector) {
        this.plugin = plugin;
        this.injector = injector;
    }

    public void startServices() {
        registerEvents();
        // 🌟 FIX: INYECCIÓN DE COMANDOS NATIVOS POR REFLEXIÓN
        registerCommands();
    }

    public void stopServices() {
        // Tareas de limpieza si son necesarias
    }

    private void registerEvents() {
        var pm = plugin.getServer().getPluginManager();
        pm.registerEvents(injector.getInstance(WoodcuttingMinigameManager.class), plugin);
        pm.registerEvents(injector.getInstance(MiningMinigameManager.class), plugin);
        pm.registerEvents(injector.getInstance(FishingHookManager.class), plugin);
        pm.registerEvents(injector.getInstance(FarmingMinigameManager.class), plugin);
        pm.registerEvents(injector.getInstance(EnchantingMinigameManager.class), plugin);
        pm.registerEvents(injector.getInstance(CombatComboManager.class), plugin);
        pm.registerEvents(injector.getInstance(AlchemyMinigameManager.class), plugin);
    }

    private void registerCommands() {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            // 💉 Inyectamos el comando saltándonos la seguridad de Paper
            commandMap.register("nexomechanics", injector.getInstance(ComandoSkillTree.class));

            plugin.getLogger().info("✅ Comando de Habilidades inyectado nativamente (Zero-Lag).");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error inyectando comando de NexoMechanics: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
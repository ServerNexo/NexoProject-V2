package me.nexo.items.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import me.nexo.items.config.ConfigManager;
import me.nexo.items.estaciones.UpgradeMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;

/**
 * 🎒 NexoItems - Comando de Forja/Upgrade (Arquitectura Enterprise)
 */
@Singleton
public class ComandoUpgrade {

    private final NexoItems plugin;
    private final ConfigManager configManager;

    @Inject
    public ComandoUpgrade(NexoItems plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * 🌟 Abre la interfaz de la Forja Omega.
     * Lamp valida automáticamente que sea un Player.
     * Si quieres restringir el comando a un permiso, descomenta la línea de abajo.
     */
    @Command({"forja", "upgrade", "upgradeitem"}) // 🌟 El comando va directamente aquí
    // @CommandPermission("nexo.items.forja.remota")
    public void openForja(Player player) {

        // Invocamos el menú inyectando la instancia del plugin
        new UpgradeMenu(player, plugin).open();

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);
    }
}
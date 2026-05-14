package me.aeroxis.mechanics.commands;

import com.google.inject.Inject;
import me.aeroxis.mechanics.AeroxisMechanics;
import me.aeroxis.mechanics.gathering.config.GatheringConfigLoader;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * ⚙️ Comando Administrativo Principal de AeroxisMechanics
 */
@Command({"nexomechanics", "mechanics"})
public class ComandoMechanics {

    private final AeroxisMechanics plugin;
    private final GatheringConfigLoader gatheringConfigLoader;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public ComandoMechanics(AeroxisMechanics plugin, GatheringConfigLoader gatheringConfigLoader) {
        this.plugin = plugin;
        this.gatheringConfigLoader = gatheringConfigLoader;
    }

    @Subcommand("reload")
    @CommandPermission("nexomechanics.admin")
    public void reload(CommandSender sender) {
        // 1. Recargamos el archivo físico config.yml
        plugin.reloadConfig();
        
        // 2. Volvemos a leer y registrar las zonas de NexoGathering en caliente
        gatheringConfigLoader.loadZones();

        sender.sendMessage(mm.deserialize("<green>⚙️ Configuración de AeroxisMechanics y zonas de recolección recargadas exitosamente.</green>"));
    }
}
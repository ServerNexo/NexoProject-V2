package me.aeroxis.cosmetics.commands;

import com.google.inject.Inject;
import me.aeroxis.cosmetics.AeroxisCosmetics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * ⚡ Comandos administrativos de AeroxisCosmetics (Framework Lamp)
 */
@Command({"aeroxiscosmetics", "cosmetics"})
public class ComandoCosmetics {

    private final AeroxisCosmetics plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public ComandoCosmetics(AeroxisCosmetics plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission("aeroxiscosmetics.admin")
    public void reload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(mm.deserialize("<green>✨ Configuración de AeroxisCosmetics recargada exitosamente.</green>"));
        // Las canciones NO necesitan recargarse aquí porque el menú las lee en tiempo real.
    }
}
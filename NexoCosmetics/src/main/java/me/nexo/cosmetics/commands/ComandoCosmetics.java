package me.nexo.cosmetics.commands;

import com.google.inject.Inject;
import me.nexo.cosmetics.NexoCosmetics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * ⚡ Comandos administrativos de NexoCosmetics (Framework Lamp)
 */
@Command({"nexocosmetics", "cosmetics"})
public class ComandoCosmetics {

    private final NexoCosmetics plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public ComandoCosmetics(NexoCosmetics plugin) {
        this.plugin = plugin;
    }

    @Subcommand("reload")
    @CommandPermission("nexocosmetics.admin")
    public void reload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(mm.deserialize("<green>✨ Configuración de NexoCosmetics recargada exitosamente.</green>"));
        // Las canciones NO necesitan recargarse aquí porque el menú las lee en tiempo real.
    }
}
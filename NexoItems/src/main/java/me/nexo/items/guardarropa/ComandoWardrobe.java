package me.nexo.items.guardarropa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 🎒 NexoItems - Comando Principal del Guardarropa (Arquitectura Enterprise)
 */
@Singleton
public class ComandoWardrobe implements CommandExecutor {

    private final NexoItems plugin;
    private final GuardarropaListener listener;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoWardrobe(NexoItems plugin, GuardarropaListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] El guardarropa solo está disponible para jugadores en el plano físico.");
            return true;
        }

        // 1. Abrir Menú (/wardrobe o /guardarropa)
        if (args.length == 0) {
            listener.abrirMenu(player);
            return true;
        }

        // 2. Ayuda (/wardrobe help)
        if (args[0].equalsIgnoreCase("help")) {
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            CrossplayUtils.sendMessage(player, "&#ff00ff👔 <bold>SISTEMA DE GUARDARROPA</bold>");
            CrossplayUtils.sendMessage(player, "&#00f5ff/wardrobe &#E6CCFF- Abre tu armario de armaduras.");
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            return true;
        }

        return true;
    }
}
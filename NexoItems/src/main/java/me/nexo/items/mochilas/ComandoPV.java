package me.nexo.items.mochilas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class ComandoPV implements CommandExecutor {

    private final NexoItems plugin;
    private final MochilaManager manager;

    private static final String ERR_NOT_PLAYER = "&#FF5555[!] El terminal requiere un operario humano.";
    private static final String ERR_USAGE = "&#FF5555[!] Uso: &#FFAA00/pv <número>";
    private static final String ERR_INVALID_NUM = "&#FF5555[!] Error: Debes ingresar un número válido.";
    private static final String ERR_NO_PERM = "&#FF5555[!] Acceso denegado a la bóveda #%num%.";

    @Inject
    public ComandoPV(NexoItems plugin, MochilaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            CrossplayUtils.sendMessage(null, ERR_NOT_PLAYER);
            return true;
        }

        // Si escribe solo "/pv", abrimos el Selector
        if (args.length == 0) {
            new PVMenu(player, plugin).open();
            return true;
        }

        if (args.length != 1) {
            CrossplayUtils.sendMessage(player, ERR_USAGE);
            return true;
        }

        try {
            int vaultNumber = Integer.parseInt(args[0]);

            // Validación de Permisos
            if (!player.hasPermission("nexo.pv." + vaultNumber) && !player.hasPermission("nexo.pv.*")) {
                CrossplayUtils.sendMessage(player, ERR_NO_PERM.replace("%num%", String.valueOf(vaultNumber)));
                return true;
            }

            // Llamada Inyectada y Segura
            manager.abrirMochila(player, vaultNumber);

        } catch (NumberFormatException e) {
            CrossplayUtils.sendMessage(player, ERR_INVALID_NUM);
        }

        return true;
    }
}
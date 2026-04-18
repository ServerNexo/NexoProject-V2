package me.nexo.items;

import me.nexo.core.utils.NexoColor;
import me.nexo.items.managers.ItemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ComandoTest implements CommandExecutor {

    private static final String ERR_NOT_PLAYER = "&#FF5555[!] El terminal requiere un operario humano.";
    private static final String ERR_USAGE = "&#FF5555[!] Uso: &#FFAA00/nexoitem <arma|armadura|herramienta> <id>";
    private static final String ERR_NOT_FOUND = "&#FF5555[!] Archivo de datos no encontrado para el ID: &#FFAA00%id%";
    private static final String MSG_SUCCESS = "&#55FF55[✓] Ítem generado e inyectado en el inventario local.";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(NexoColor.parse(ERR_USAGE));
            return true;
        }

        String type = args[0].toLowerCase();
        String id = args[1];
        ItemStack item = null;

        if (type.equals("arma")) item = ItemManager.generarArmaRPG(id);
        else if (type.equals("armadura")) item = ItemManager.generarArmadura(id);
        else if (type.equals("herramienta")) item = ItemManager.generarHerramienta(id);

        if (item == null) {
            player.sendMessage(NexoColor.parse(ERR_NOT_FOUND.replace("%id%", id)));
            return true;
        }

        player.getInventory().addItem(item);
        player.sendMessage(NexoColor.parse(MSG_SUCCESS));
        return true;
    }
}
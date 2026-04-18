package me.nexo.items.accesorios;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 🎒 NexoItems - Comando Principal de Accesorios (Arquitectura Enterprise)
 */
@Singleton
public class ComandoAccesorios implements CommandExecutor {

    private final NexoItems plugin;
    private final AccesoriosManager manager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoAccesorios(NexoItems plugin, AccesoriosManager manager) {
        this.plugin = plugin;
        this.manager = manager; // Inyectamos el manager directo a la vena
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] Comando solo disponible para jugadores en el plano físico.");
            return true;
        }

        // 1. Abrir Bóveda (/accesorios)
        if (args.length == 0) {
            manager.abrirBolsa(player);
            return true;
        }

        // 2. Ayuda (/accesorios help)
        if (args[0].equalsIgnoreCase("help")) {
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            CrossplayUtils.sendMessage(player, "&#ff00ff<bold>💍 SISTEMA DE ACCESORIOS</bold>");
            CrossplayUtils.sendMessage(player, "&#00f5ff/accesorios &#E6CCFF- Abre tu bóveda de accesorios.");

            if (player.hasPermission("nexo.admin")) {
                CrossplayUtils.sendMessage(player, "&#00f5ff/accesorios give <jugador> <id> &#E6CCFF- Concede un accesorio.");
            }

            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            return true;
        }

        // 3. Dar Accesorio (Admin - /accesorios give <jugador> <id>)
        if (args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("nexo.admin")) {
                CrossplayUtils.sendMessage(player, "&#FF3366[!] El Vacío rechaza tu petición (Sin Permisos).");
                return true;
            }

            if (args.length < 3) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso incorrecto: /accesorios give <jugador> <id>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                CrossplayUtils.sendMessage(player, "&#FF3366[!] Ese jugador no se encuentra en este plano.");
                return true;
            }

            String accId = args[2].toLowerCase();
            org.bukkit.inventory.ItemStack item = manager.generarAccesorio(accId);

            if (item == null) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Anomalía detectada: El accesorio '" + accId + "' no existe en el registro.");
                return true;
            }

            target.getInventory().addItem(item);
            CrossplayUtils.sendMessage(target, "&#00f5ff[📦] Has recibido un nuevo Accesorio de poder.");
            CrossplayUtils.sendMessage(player, "&#00f5ff[📦] Accesorio entregado con éxito a " + target.getName() + ".");
            return true;
        }

        return true;
    }
}
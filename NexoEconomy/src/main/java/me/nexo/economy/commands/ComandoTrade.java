package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.trade.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 💰 NexoEconomy - Comando de Intercambios (Arquitectura Enterprise)
 */
@Singleton
public class ComandoTrade implements CommandExecutor {

    private final NexoEconomy plugin;
    private final TradeManager tradeManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoTrade(NexoEconomy plugin, TradeManager tradeManager) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 🌟 FILTRO DE CONSOLA SEGURO (Evita NullPointerException)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] La consola no posee un inventario físico para realizar intercambios.");
            return true;
        }

        // USO BASE
        if (args.length == 0) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: &#00f5ff/trade <jugador> &#FF5555o &#00f5ff/trade accept <jugador>");
            return true;
        }

        // ACEPTAR PETICIÓN
        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: &#00f5ff/trade accept <jugador>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] El jugador no está en línea o no existe.");
                return true;
            }

            if (tradeManager.tienePeticionDe(player, target)) {
                tradeManager.iniciarTrade(player, target);
            } else {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] No tienes ninguna petición de intercambio pendiente de &#00f5ff" + target.getName() + "&#FF5555.");
            }
            return true;
        }

        // ENVIAR PETICIÓN (Asumimos que el Arg 0 es el nombre de un jugador)
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] El jugador no está en línea o no existe.");
            return true;
        }

        // BLOQUEO DE AUTO-COMERCIO
        if (target.getUniqueId().equals(player.getUniqueId())) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] No puedes comerciar contigo mismo. El Vacío no permite paradojas.");
            return true;
        }

        tradeManager.enviarPeticion(player, target);
        return true;
    }
}
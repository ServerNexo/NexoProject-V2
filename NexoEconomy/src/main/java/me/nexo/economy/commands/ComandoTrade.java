package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.trade.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 💰 NexoEconomy - Comando de Intercambios (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado rápido, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoTrade extends Command {

    private final NexoEconomy plugin;
    private final TradeManager tradeManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoTrade(NexoEconomy plugin, TradeManager tradeManager) {
        super("trade"); // 🌟 Nombre nativo
        this.setAliases(List.of("intercambio", "intercambiar")); // Alias adicionales

        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 FILTRO DE CONSOLA SEGURO
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

        // ENVIAR PETICIÓN
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

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        // Si escribe el primer argumento, sugerimos "accept" y los jugadores online
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("accept");
            Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));

            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // Si escribe "accept", le sugerimos los jugadores online para que sepa de quién aceptar
        if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
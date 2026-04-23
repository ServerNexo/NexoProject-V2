package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
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
 * 💰 NexoEconomy - Comando de Intercambios (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap Nativo, Cero Estáticos e Inyección de Dependencias Estricta.
 */
@Singleton
public class ComandoTrade extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final TradeManager tradeManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ComandoTrade(TradeManager tradeManager, CrossplayUtils crossplayUtils) {
        super("trade");
        this.description = "Sistema de intercambio seguro entre jugadores.";
        this.aliases = List.of("intercambiar", "comerciar");

        this.tradeManager = tradeManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 FILTRO DE CONSOLA SEGURO
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] La consola no posee un inventario físico para realizar intercambios.");
            return true;
        }

        // USO BASE
        if (args.length == 0) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: &#00f5ff/trade <jugador> &#FF5555o &#00f5ff/trade accept <jugador>");
            return true;
        }

        // ACEPTAR PETICIÓN
        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: &#00f5ff/trade accept <jugador>");
                return true;
            }

            var target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] El jugador no está en línea o no existe.");
                return true;
            }

            if (tradeManager.tienePeticionDe(player, target)) {
                tradeManager.iniciarTrade(player, target);
            } else {
                crossplayUtils.sendMessage(player, "&#FF5555[!] No tienes ninguna petición de intercambio pendiente de &#00f5ff" + target.getName() + "&#FF5555.");
            }
            return true;
        }

        // ENVIAR PETICIÓN (Asumimos que el Arg 0 es el nombre de un jugador)
        var target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] El jugador no está en línea o no existe.");
            return true;
        }

        // BLOQUEO DE AUTO-COMERCIO
        if (target.getUniqueId().equals(player.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] No puedes comerciar contigo mismo. El Vacío no permite paradojas.");
            return true;
        }

        tradeManager.enviarPeticion(player, target);
        return true;
    }

    // 🌟 OPTIMIZACIÓN DE AUTOCOMPLETADO (Evita tirones de TPS al escribir comandos)
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            // Sugerir "accept" o nombres de jugadores en línea
            var suggestions = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList());
                    
            if ("accept".startsWith(args[0].toLowerCase())) {
                suggestions.add("accept");
            }
            return suggestions;
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        
        return Collections.emptyList();
    }
}
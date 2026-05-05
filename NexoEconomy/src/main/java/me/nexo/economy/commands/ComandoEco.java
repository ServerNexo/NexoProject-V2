package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.config.ConfigManager;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 💰 NexoEconomy - Comando Principal de Economía (Arquitectura Enterprise Java 21)
 * Rendimiento: Fusión de TabCompleter, CommandMap Nativo, Setters Seguros y Anti-Leaks Asíncronos.
 */
@Singleton
public class ComandoEco extends Command {

    private static final List<String> CURRENCIES = List.of("COINS", "GEMS", "MANA");
    private static final List<String> AMOUNTS = List.of("100", "500", "1000", "5000", "10000");

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final EconomyManager economyManager;
    private final CrossplayUtils crossplayUtils;
    private final ConfigManager configManager;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ComandoEco(EconomyManager economyManager, CrossplayUtils crossplayUtils, ConfigManager configManager) {
        super("eco");

        this.setDescription("Consulta tu balance bancario o inyecta fondos.");
        this.setAliases(List.of("economia", "balance", "money"));

        this.economyManager = economyManager;
        this.crossplayUtils = crossplayUtils;
        this.configManager = configManager;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 VER BALANCE PROPIO (Exclusivo para Jugadores)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(configManager.getMessages().comandos().noJugador());
                return true;
            }

            var accOpt = economyManager.getCachedAccount(player.getUniqueId(), NexoAccount.AccountType.PLAYER);

            if (accOpt.isPresent()) {
                var acc = accOpt.get();
                crossplayUtils.sendMessage(player, "&#555555--------------------------------");
                crossplayUtils.sendMessage(player, "&#00f5ff🏦 <bold>ESTADO DE CUENTA: " + player.getName() + "</bold>");

                String symbol = configManager.getMessages().general().monedaSimbolo();
                crossplayUtils.sendMessage(player, "&#FFAA00Monedas: &#E6CCFF" + acc.getCoins().toPlainString() + " " + symbol);
                crossplayUtils.sendMessage(player, "&#55FF55Gemas: &#E6CCFF" + acc.getGems().toPlainString() + " 💎");
                crossplayUtils.sendMessage(player, "&#ff00ffManá: &#E6CCFF" + acc.getMana().toPlainString() + " 💧");
                crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            } else {
                crossplayUtils.sendMessage(player, "&#FFAA00[⏳] Sincronizando con el Banco Central... Inténtalo de nuevo en unos segundos.");
            }
            return true;
        }

        // 🌟 COMANDO DE ADMINISTRADOR: INYECTAR FONDOS (Abierto a Consola)
        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("nexoeconomy.admin")) {
                sendConsoleSafeMessage(sender, configManager.getMessages().comandos().sinPermiso());
                return true;
            }

            if (args.length < 4) {
                sendConsoleSafeMessage(sender, "&#FF5555[!] Uso correcto: /eco give <jugador> <COINS|GEMS|MANA> <cantidad>");
                return true;
            }

            var target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sendConsoleSafeMessage(sender, "&#FF5555[!] El jugador no está en línea o no existe.");
                return true;
            }

            NexoAccount.Currency currency;
            try {
                currency = NexoAccount.Currency.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sendConsoleSafeMessage(sender, "&#FF5555[!] Divisa inválida. Usa COINS, GEMS o MANA.");
                return true;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(args[3]);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    sendConsoleSafeMessage(sender, "&#FF5555[!] La cantidad debe ser mayor a 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sendConsoleSafeMessage(sender, "&#FF5555[!] Cantidad inválida. Solo se permiten valores numéricos.");
                return true;
            }

            // 🛡️ PREVENCIÓN MEMORY LEAK Y ASIGNACIÓN SEGURA
            var targetId = target.getUniqueId();
            var targetName = target.getName();

            // Verificamos si el emisor es la Consola o un Jugador para devolver el mensaje correctamente
            final boolean isPlayerSender = sender instanceof Player;
            final UUID senderId = isPlayerSender ? ((Player) sender).getUniqueId() : null;

            // Transacción asíncrona segura
            economyManager.updateBalanceAsync(targetId, NexoAccount.AccountType.PLAYER, currency, amount, true).thenAccept(success -> {
                // Volvemos a buscar a los jugadores en la RAM por si se desconectaron
                var onlineTarget = Bukkit.getPlayer(targetId);

                if (success) {
                    // Notificar al emisor
                    if (isPlayerSender) {
                        var onlineSender = Bukkit.getPlayer(senderId);
                        if (onlineSender != null) crossplayUtils.sendMessage(onlineSender, "&#55FF55[✓] <bold>TRANSFERENCIA:</bold> &#E6CCFFHas emitido " + amount.toPlainString() + " " + currency.name() + " a la cuenta de " + targetName + ".");
                    } else {
                        sender.sendMessage("§a[NexoEconomy] Has emitido " + amount.toPlainString() + " " + currency.name() + " a la cuenta de " + targetName + ".");
                    }

                    // Notificar al receptor
                    if (onlineTarget != null) crossplayUtils.sendMessage(onlineTarget, "&#55FF55[+] <bold>DEPÓSITO BANCARIO:</bold> &#E6CCFFHas recibido " + amount.toPlainString() + " " + currency.name() + ".");
                } else {
                    if (isPlayerSender) {
                        var onlineSender = Bukkit.getPlayer(senderId);
                        if (onlineSender != null) crossplayUtils.sendMessage(onlineSender, "&#FF5555[!] Error crítico en la transacción atómica. La operación fue rechazada por seguridad.");
                    } else {
                        sender.sendMessage("§c[NexoEconomy] Error crítico en la transacción atómica.");
                    }
                }
            });
            return true;
        }

        sendConsoleSafeMessage(sender, "&#FF5555[!] Comando desconocido. Usa /eco para ver tu balance.");
        return true;
    }

    // 🛠️ MÉTODO DE SOPORTE PARA MENSAJES HÍBRIDOS (CONSOLA / JUGADOR)
    private void sendConsoleSafeMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            crossplayUtils.sendMessage(player, message);
        } else {
            // Limpiamos los colores Hexadecimales para que la consola los lea en formato limpio
            String cleanMsg = message.replace("&#FF5555", "§c")
                    .replace("&#55FF55", "§a")
                    .replace("<bold>", "")
                    .replace("</bold>", "");
            sender.sendMessage(cleanMsg);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("nexoeconomy.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return CURRENCIES.stream()
                    .filter(s -> s.startsWith(args[2].toUpperCase()))
                    .toList();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return AMOUNTS.stream()
                    .filter(s -> s.startsWith(args[3]))
                    .toList();
        }

        return Collections.emptyList();
    }
}
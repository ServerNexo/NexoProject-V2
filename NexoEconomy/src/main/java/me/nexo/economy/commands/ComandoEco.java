package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
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
import java.util.Optional;

/**
 * 💰 NexoEconomy - Comando Principal de Economía (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoEco extends Command {

    private final NexoEconomy plugin;
    private final EconomyManager economyManager;

    // 🌟 FIX: Listas estáticas inmutables de Java 21 para el autocompletado
    private static final List<String> CURRENCIES = List.of("COINS", "GEMS", "MANA");
    private static final List<String> AMOUNTS = List.of("100", "500", "1000", "5000", "10000");

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoEco(NexoEconomy plugin, EconomyManager economyManager) {
        super("eco"); // 🌟 Nombre nativo
        this.setAliases(List.of("economy", "money", "balance")); // Alias extra

        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] La consola no posee una cuenta bancaria.");
            return true;
        }

        // 🌟 VER BALANCE PROPIO
        if (args.length == 0) {
            Optional<NexoAccount> accOpt = economyManager.getCachedAccount(player.getUniqueId(), NexoAccount.AccountType.PLAYER);

            if (accOpt.isPresent()) {
                NexoAccount acc = accOpt.get();
                CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
                CrossplayUtils.sendMessage(player, "&#00f5ff🏦 <bold>ESTADO DE CUENTA: " + player.getName() + "</bold>");
                CrossplayUtils.sendMessage(player, "&#FFAA00Monedas: &#E6CCFF" + acc.getCoins().toString() + " ⛃");
                CrossplayUtils.sendMessage(player, "&#55FF55Gemas: &#E6CCFF" + acc.getGems().toString() + " 💎");
                CrossplayUtils.sendMessage(player, "&#ff00ffManá: &#E6CCFF" + acc.getMana().toString() + " 💧");
                CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            } else {
                CrossplayUtils.sendMessage(player, "&#FFAA00[⏳] Sincronizando con el Banco Central... Inténtalo de nuevo en unos segundos.");
            }
            return true;
        }

        // 🌟 COMANDO DE ADMINISTRADOR: INYECTAR FONDOS
        if (args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("nexoeconomy.admin")) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] El Vacío rechaza tu petición (Sin Permisos).");
                return true;
            }

            if (args.length < 4) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /eco give <jugador> <COINS|GEMS|MANA> <cantidad>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] El jugador no está en línea o no existe.");
                return true;
            }

            NexoAccount.Currency currency;
            try {
                currency = NexoAccount.Currency.valueOf(args[2].toUpperCase());
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Divisa inválida. Usa COINS, GEMS o MANA.");
                return true;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(args[3]);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] La cantidad debe ser mayor a 0.");
                    return true;
                }
            } catch (Exception e) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Cantidad inválida. Solo se permiten valores numéricos.");
                return true;
            }

            // Transacción asíncrona segura
            economyManager.updateBalanceAsync(target.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amount, true).thenAccept(success -> {
                if (success) {
                    CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>TRANSFERENCIA:</bold> &#E6CCFFHas emitido " + amount.toString() + " " + currency.name() + " a la cuenta de " + target.getName() + ".");
                    CrossplayUtils.sendMessage(target, "&#55FF55[+] <bold>DEPÓSITO BANCARIO:</bold> &#E6CCFFHas recibido " + amount.toString() + " " + currency.name() + ".");
                } else {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Error crítico en la transacción atómica. La operación fue rechazada por seguridad.");
                }
            });
            return true;
        }

        // Si pone cualquier otra cosa
        CrossplayUtils.sendMessage(player, "&#FF5555[!] Comando desconocido. Usa /eco para ver tu balance.");
        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        // 🛡️ SEGURIDAD VISUAL: Si no es administrador, no le sugerimos nada.
        if (!sender.hasPermission("nexoeconomy.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList(); // 🌟 FIX: Optimizamos a .toList() nativo
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
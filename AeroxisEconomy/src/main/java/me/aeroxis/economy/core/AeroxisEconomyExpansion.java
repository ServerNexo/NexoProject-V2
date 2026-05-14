package me.aeroxis.economy.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class AeroxisEconomyExpansion extends PlaceholderExpansion {

    private final EconomyManager economyManager;
    private final DecimalFormat formatter = new DecimalFormat("#,##0.00");

    public AeroxisEconomyExpansion(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public @NotNull String getIdentifier() { return "nexoeconomy"; }

    @Override
    public @NotNull String getAuthor() { return "Nexo"; }

    @Override
    public @NotNull String getVersion() { return "1.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        // Dividimos el parámetro. Ej: "coins_formatted" -> ["coins", "formatted"]
        String[] parts = params.toLowerCase().split("_");
        if (parts.length == 0) return null;

        String currencyStr = parts[0]; // "coins", "gems" o "mana"
        AeroxisAccount.Currency currency;

        try {
            currency = AeroxisAccount.Currency.valueOf(currencyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // Si escriben %nexoeconomy_diamantes%, lo ignoramos.
        }

        // Leemos el dinero del caché usando el método nuevo
        BigDecimal balance = economyManager.getBalanceSync(player.getUniqueId(), AeroxisAccount.AccountType.PLAYER, currency);
        double amount = balance.doubleValue();

        // Verificamos si pidieron un sufijo de formato (_formatted o _k)
        if (parts.length > 1) {
            String format = parts[1];
            if (format.equals("formatted")) {
                return formatter.format(amount); // Ej: 1,500.50
            } else if (format.equals("k")) {
                return formatK(amount); // Ej: 1.5k
            }
        }

        // Si solo pusieron %nexoeconomy_coins%, devolvemos el número crudo
        return String.valueOf(amount);
    }

    // Método para dar formato MMO (k, M, B)
    private String formatK(double amount) {
        if (amount >= 1_000_000_000) return String.format("%.2fB", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format("%.2fM", amount / 1_000_000.0);
        if (amount >= 1_000) return String.format("%.2fk", amount / 1_000.0);
        return String.format("%.2f", amount);
    }
}
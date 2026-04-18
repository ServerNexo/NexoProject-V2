package me.nexo.economy.blackmarket;

import me.nexo.economy.core.NexoAccount;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public record BlackMarketItem(
        String internalId,      // Identificador para tu código
        ItemStack displayItem,  // El ítem visual que se mostrará y entregará
        BigDecimal price,       // El costo
        NexoAccount.Currency currency // GEMS o MANA (¡Nunca Monedas!)
) {
}
package me.nexo.economy.bazar;

import java.math.BigDecimal;
import java.util.UUID;

public record BazaarOrder(
        UUID orderId,
        UUID ownerId,
        OrderType type,
        String itemId, // El identificador del ítem (ej: "DIAMOND" o "nexoitems:espada_oscura")
        int amount,
        BigDecimal pricePerUnit,
        long timestamp
) {
    public enum OrderType {
        BUY, SELL
    }

    // Calcula el costo/valor total de esta orden
    public BigDecimal getTotalPrice() {
        return pricePerUnit.multiply(new BigDecimal(amount));
    }
}
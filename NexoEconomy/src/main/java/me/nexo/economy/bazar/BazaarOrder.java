package me.nexo.economy.bazar;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * 💰 NexoEconomy - DTO de Orden del Bazar (Arquitectura Enterprise Java 21)
 * Rendimiento: Inmutabilidad profunda, Validaciones Fail-Fast y Optimización de caché numérico.
 * Nota: Es puramente un contenedor de datos, no requiere @Singleton.
 */
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

    // 🌟 VALIDACIÓN DEFENSIVA Y FAIL-FAST
    // Previene la inyección de datos corruptos desde comandos o la base de datos.
    public BazaarOrder {
        Objects.requireNonNull(orderId, "El ID de la orden no puede ser nulo.");
        Objects.requireNonNull(ownerId, "El UUID del dueño no puede ser nulo.");
        Objects.requireNonNull(type, "El tipo de orden no puede ser nulo.");
        Objects.requireNonNull(itemId, "El ID del ítem no puede ser nulo.");
        Objects.requireNonNull(pricePerUnit, "El precio unitario no puede ser nulo.");

        if (amount <= 0) {
            throw new IllegalArgumentException("La cantidad de la orden debe ser mayor a 0.");
        }
        if (pricePerUnit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio por unidad no puede ser negativo.");
        }
    }

    /**
     * Calcula el costo/valor total de esta orden.
     */
    public BigDecimal getTotalPrice() {
        // 🌟 OPTIMIZACIÓN: valueOf() reutiliza caché de memoria interno de la JVM
        return pricePerUnit.multiply(BigDecimal.valueOf(amount));
    }
}
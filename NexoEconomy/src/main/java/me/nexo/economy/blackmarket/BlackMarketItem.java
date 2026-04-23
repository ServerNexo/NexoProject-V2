package me.nexo.economy.blackmarket;

import me.nexo.economy.core.NexoAccount;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 💰 NexoEconomy - Entidad de Ítem Clandestino (Data Transfer Object)
 * Arquitectura: Java 21 Records con Inmutabilidad Defensiva.
 * Nota: Es puramente un contenedor de datos, no requiere @Singleton.
 */
public record BlackMarketItem(
        String internalId,      // Identificador interno
        ItemStack displayItem,  // El ítem visual que se mostrará y entregará
        BigDecimal price,       // El costo
        NexoAccount.Currency currency // GEMS o MANA
) {
    // 🌟 VALIDACIÓN Y CLONACIÓN DEFENSIVA
    public BlackMarketItem {
        Objects.requireNonNull(internalId, "El ID interno no puede ser nulo");
        Objects.requireNonNull(displayItem, "El ItemStack no puede ser nulo");
        Objects.requireNonNull(price, "El precio no puede ser nulo");
        Objects.requireNonNull(currency, "La divisa no puede ser nula");

        // Nos aseguramos de guardar una copia exacta, desconectada de quien la creó
        displayItem = displayItem.clone();
    }

    /**
     * 🛡️ INMUTABILIDAD ESTRICTA: 
     * Sobrescribimos el acceso nativo del Record para que siempre devuelva un clon.
     * Esto evita que otros hilos o menús modifiquen el stock global accidentalmente.
     */
    @Override
    public ItemStack displayItem() {
        return displayItem.clone();
    }
}
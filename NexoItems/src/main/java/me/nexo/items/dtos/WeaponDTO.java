package me.nexo.items.dtos;

/**
 * 🎒 NexoItems - DTO de Arma RPG (Arquitectura Enterprise Java 21)
 * Estructura de datos inmutable y Thread-Safe.
 */
public record WeaponDTO(
        String id,
        String nombre,
        int tier,
        String claseRequerida,
        String elemento,
        int nivelRequerido,
        double danioBase,
        double velocidadAtaque,
        String habilidadId,
        boolean permitePrestigio,
        double multiPrestigio
) {}
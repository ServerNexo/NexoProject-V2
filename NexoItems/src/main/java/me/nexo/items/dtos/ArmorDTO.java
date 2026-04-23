package me.nexo.items.dtos;

/**
 * 🎒 NexoItems - DTO de Armadura RPG (Arquitectura Enterprise Java 21)
 * Estructura de datos de inmutabilidad profunda (Primitivos y Strings) y Thread-Safe.
 */
public record ArmorDTO(
        String id,
        String nombre,
        String claseRequerida,
        String skillRequerida,
        int nivelRequerido,
        double vidaExtra,
        double velocidadMovimiento,
        double suerteMinera,
        double velocidadMineria,
        double suerteAgricola,
        double suerteTala,
        double criaturaMarina,
        double velocidadPesca
) {}
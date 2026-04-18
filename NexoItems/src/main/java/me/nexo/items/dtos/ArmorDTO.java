package me.nexo.items.dtos;

// Usamos record (Java 14+) porque son súper ligeros y rápidos para guardar datos fijos
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
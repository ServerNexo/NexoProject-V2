package me.nexo.items.dtos;

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
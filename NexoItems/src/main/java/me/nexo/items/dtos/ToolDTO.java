package me.nexo.items.dtos;

public record ToolDTO(
        String id,
        String nombre,
        String rareza,
        String profesion,
        int tier,
        int nivelRequerido,
        double velocidadBase,
        double multiplicadorFortuna,
        String habilidadId
) {
    public boolean esTaladro() {
        return profesion.equalsIgnoreCase("Minería") && tier >= 3;
    }

    public boolean esEvolutiva() {
        return id.equalsIgnoreCase("azada_matematica");
    }
}
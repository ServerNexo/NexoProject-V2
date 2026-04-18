package me.nexo.items.dtos;

import java.util.List;

public record ReforgeDTO(
        String id,
        String nombre,
        String prefijoColor,
        List<String> clasesAplicables,
        int costoPolvo,
        double danioExtra,
        double velocidadAtaqueExtra,
        double fortunaExtra // ⬅️ ¡Aquí está la magia que faltaba!
) {
    public boolean aplicaAClase(String claseJugador) {
        if (claseJugador == null) return false;
        for (String clase : clasesAplicables) {
            if (clase.equalsIgnoreCase(claseJugador) || clase.equalsIgnoreCase("Cualquiera")) {
                return true;
            }
        }
        return false;
    }
}
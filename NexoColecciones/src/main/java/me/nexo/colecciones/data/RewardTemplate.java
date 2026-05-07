package me.nexo.colecciones.data;

import java.util.List;

/**
 * 🎁 Plantilla de Recompensas Reutilizable (Flyweight Pattern)
 * Almacena el diseño (lore) y la lógica (comandos) genéricos para ser
 * referenciados por múltiples niveles (Tiers) sin duplicar memoria.
 */
public record RewardTemplate(
        String id,
        List<String> lore,
        List<String> comandos
) {}
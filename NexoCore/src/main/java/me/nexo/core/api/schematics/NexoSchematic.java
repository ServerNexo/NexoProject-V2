package me.nexo.core.api.schematics;

import org.bukkit.block.data.BlockData;

/**
 * 📦 Estructura de Memoria Optimizada para Esquemáticas (Folia-Ready).
 * Usa un Array Unidimensional para máxima eficiencia en la caché del CPU (Cache Locality).
 */
public record NexoSchematic(
        String id,
        int width,
        int height,
        int length,
        BlockData[] blocks
) {
    /**
     * Convierte coordenadas tridimensionales en un índice unidimensional ultra rápido.
     */
    public int getIndex(int x, int y, int z) {
        return x + width * (y + height * z);
    }
}
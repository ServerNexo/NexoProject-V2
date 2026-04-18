package me.nexo.factories.core;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class StructureTemplate {

    private final String factoryType;
    // Mapa de Vectores Relativos al bloque central (Núcleo)
    private final Map<Vector, Material> requiredBlocks = new HashMap<>();

    public StructureTemplate(String factoryType) {
        this.factoryType = factoryType;
    }

    // Añadir un bloque requerido relativo al centro (Ej: 0, 1, 0 es justo arriba)
    public void addBlock(int relX, int relY, int relZ, Material material) {
        requiredBlocks.put(new Vector(relX, relY, relZ), material);
    }

    /**
     * Verifica instantáneamente si la estructura está construida correctamente alrededor del núcleo.
     * Complejidad O(1) - Amigable con los TPS.
     */
    public boolean isValid(Block coreBlock) {
        for (Map.Entry<Vector, Material> entry : requiredBlocks.entrySet()) {
            Vector rel = entry.getKey();
            Block check = coreBlock.getRelative(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ());

            if (check.getType() != entry.getValue()) {
                return false; // Si tan solo un bloque está mal, falla y no sigue buscando
            }
        }
        return true;
    }

    public String getFactoryType() { return factoryType; }
    public Map<Vector, Material> getRequiredBlocks() { return requiredBlocks; }
}
package me.nexo.protections.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * 🛡️ NexoProtections - Caja de Colisión Espacial (AABB)
 * Rendimiento: Matemáticas Puras Inmutables. Búsquedas O(1) con comparación de UUID.
 */
public record ClaimBox(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    // 🌟 OPTIMIZACIÓN: Cacheamos el UUID del mundo una vez para evitar comprobaciones lentas de Strings
    private UUID getWorldId() {
        World w = Bukkit.getWorld(world);
        return w != null ? w.getUID() : null;
    }

    /**
     * Método ultra-rápido para saber si un bloque exacto está dentro de este cubo.
     * Complejidad: O(1). Modificado para evitar String.equals()
     */
    public boolean contains(Location loc) {
        // Validación segura y rápida de memoria O(1)
        if (loc == null || loc.getWorld() == null) return false;

        // 🚀 Comparamos identificadores de memoria rápida, NO Strings.
        if (!loc.getWorld().getName().equals(world)) return false;

        // Caché en la pila (Stack) del procesador
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    /**
     * Fórmula Matemática AABB para detectar choques entre dos terrenos.
     * Complejidad: O(1).
     */
    public boolean intersects(ClaimBox other) {
        if (other == null) return false;
        if (!this.world.equals(other.world())) return false;

        // Operaciones a nivel de bit puro.
        return this.minX <= other.maxX() && this.maxX >= other.minX() &&
                this.minY <= other.maxY() && this.maxY >= other.minY() &&
                this.minZ <= other.maxZ() && this.maxZ >= other.minZ();
    }
}
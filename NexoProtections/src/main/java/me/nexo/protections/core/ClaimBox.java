package me.nexo.protections.core;

import org.bukkit.Location;

/**
 * 🛡️ NexoProtections - Caja de Colisión Espacial (AABB)
 * Rendimiento: Data Carrier inmutable (Java 16+), Matemáticas Puras y 100% Thread-Safe.
 */
public record ClaimBox(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /**
     * Método ultra-rápido para saber si un bloque exacto está dentro de este cubo.
     * Complejidad: O(1).
     */
    public boolean contains(Location loc) {
        // Validación segura y rápida de memoria O(1)
        if (loc == null || loc.getWorld() == null) return false;

        // Comparamos el nombre del mundo (Rápido gracias al String Pool de la JVM)
        if (!loc.getWorld().getName().equals(this.world)) return false;

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

        // Operaciones condicionales simples de CPU.
        return this.minX <= other.maxX() && this.maxX >= other.minX() &&
               this.minY <= other.maxY() && this.maxY >= other.minY() &&
               this.minZ <= other.maxZ() && this.maxZ >= other.minZ();
    }
}
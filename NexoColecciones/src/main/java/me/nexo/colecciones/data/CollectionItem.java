package me.nexo.colecciones.data;

import org.bukkit.Material;

import java.util.Map;

/**
 * 📚 NexoColecciones - Objeto de Ítem de Colección (DTO)
 * Rendimiento: Convertido a Record inmutable (Java 16+). Map.copyOf para Inmutabilidad Real.
 * Nota: Es instanciado en RAM por el Manager. No requiere Inyección de Dependencias.
 */
public record CollectionItem(String id, String categoriaId, String nombre, Material icono, String nexoId, int slotMenu, Map<Integer, Tier> tiers, int maxTier) {

    // 🌟 FIX: Constructor personalizado para mantener tu pre-cálculo O(1) de la clase original
    public CollectionItem(String id, String categoriaId, String nombre, Material icono, String nexoId, int slotMenu, Map<Integer, Tier> tiers) {
        this(
            id, 
            categoriaId, 
            nombre, 
            icono, 
            nexoId, 
            slotMenu,
            // 🌟 OPTIMIZACIÓN JAVA 10+: Map.copyOf es más rápido y seguro que Collections.unmodifiableMap
            tiers != null ? Map.copyOf(tiers) : Map.of(), 
            // 🌟 Mantiene tu pre-cálculo en RAM para consultas O(1) instantáneas
            tiers != null ? tiers.keySet().stream().max(Integer::compareTo).orElse(0) : 0
        );
    }

    // ==========================================
    // 💡 GETTERS DE COMPATIBILIDAD (LEGACY BRIDGE)
    // Para no romper las llamadas existentes en menús y managers.
    // ==========================================
    
    public String getId() { return id(); }
    public String getCategoriaId() { return categoriaId(); }
    public String getNombre() { return nombre(); }
    public Material getIcono() { return icono(); }
    public String getNexoId() { return nexoId(); }
    public int getSlotMenu() { return slotMenu(); }

    public Map<Integer, Tier> getTiers() {
        return tiers(); // Ya es inmutable gracias a Map.copyOf()
    }

    public Tier getTier(int nivel) {
        return tiers().get(nivel);
    }

    // 🌟 Acceso O(1) instantáneo garantizado
    public int getMaxTier() {
        return maxTier();
    }
}
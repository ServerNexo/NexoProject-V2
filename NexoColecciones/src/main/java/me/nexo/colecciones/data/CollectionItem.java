package me.nexo.colecciones.data;

import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

/**
 * 📚 NexoColecciones - Objeto de Ítem de Colección (DTO)
 * Nota: Es instanciado en RAM. No requiere Inyección de Dependencias.
 */
public class CollectionItem {
    private final String id;
    private final String categoriaId;
    private final String nombre;
    private final Material icono;
    private final String nexoId;
    private final int slotMenu;

    private final Map<Integer, Tier> tiers;
    private final int maxTier; // 🌟 OPTIMIZACIÓN: Nivel máximo precalculado en RAM

    public CollectionItem(String id, String categoriaId, String nombre, Material icono, String nexoId, int slotMenu, Map<Integer, Tier> tiers) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.icono = icono;
        this.nexoId = nexoId;
        this.slotMenu = slotMenu;

        // 🌟 FIX: Sellamos el mapa desde el constructor para que sea 100% inmutable
        this.tiers = tiers != null ? Collections.unmodifiableMap(tiers) : Collections.emptyMap();

        // 🌟 FIX: Pre-calculamos el nivel máximo 1 sola vez en lugar de usar Streams cada vez
        this.maxTier = this.tiers.keySet().stream().max(Integer::compareTo).orElse(0);
    }

    public String getId() { return id; }
    public String getCategoriaId() { return categoriaId; }
    public String getNombre() { return nombre; }
    public Material getIcono() { return icono; }
    public String getNexoId() { return nexoId; }
    public int getSlotMenu() { return slotMenu; }

    public Map<Integer, Tier> getTiers() {
        return tiers; // Ya es inmutable gracias al constructor
    }

    public Tier getTier(int nivel) {
        return tiers.get(nivel);
    }

    // 🌟 Acceso O(1) instantáneo (Cero consumo de CPU extra)
    public int getMaxTier() {
        return maxTier;
    }
}
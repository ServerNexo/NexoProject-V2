package me.nexo.colecciones.data;

import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📚 NexoColecciones - Objeto de Categoría en Memoria (DTO)
 * Rendimiento: Vistas Inmutables Cacheadas (Zero-Garbage O(1)).
 * Nota: Es instanciado por el Config/Manager. No requiere Inyección de Dependencias.
 */
public class CollectionCategory {

    private final String id;
    private final String nombre;
    private final Material icono;
    private final int slot;

    // 🌟 FIX: Usamos ConcurrentHashMap para lectura concurrente segura por múltiples jugadores
    private final Map<String, CollectionItem> items = new ConcurrentHashMap<>();
    
    // 🌟 FIX ZERO-GARBAGE: Cacheamos la vista inmutable al nacer.
    // Evita la creación de miles de objetos de envoltura durante el renderizado de menús.
    private final Map<String, CollectionItem> unmodifiableItems = Collections.unmodifiableMap(items);

    public CollectionCategory(String id, String nombre, Material icono, int slot) {
        this.id = id;
        this.nombre = nombre;
        this.icono = icono;
        this.slot = slot;
    }

    public void addItem(CollectionItem item) {
        items.put(item.getId(), item);
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public Material getIcono() { return icono; }
    public int getSlot() { return slot; }

    // 🌟 FIX: Retornamos la referencia cacheada en lugar de instanciar una nueva.
    public Map<String, CollectionItem> getItems() {
        return unmodifiableItems;
    }
}
package me.nexo.colecciones.data;

import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📚 NexoColecciones - Objeto de Categoría en Memoria (DTO)
 * Nota: Es instanciado por el Config. No requiere Inyección de Dependencias.
 */
public class CollectionCategory {

    private final String id;
    private final String nombre;
    private final Material icono;
    private final int slot;

    // 🌟 FIX: Usamos ConcurrentHashMap para lectura concurrente segura por múltiples jugadores
    private final Map<String, CollectionItem> items = new ConcurrentHashMap<>();

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

    // 🌟 FIX: Retornamos un mapa INMUTABLE.
    // Nadie podrá modificar, añadir o borrar ítems accidentalmente desde fuera de esta clase.
    public Map<String, CollectionItem> getItems() {
        return Collections.unmodifiableMap(items);
    }
}
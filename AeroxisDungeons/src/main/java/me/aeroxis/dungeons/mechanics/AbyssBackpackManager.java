package me.aeroxis.dungeons.mechanics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.dungeons.AeroxisDungeons;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🎒 AeroxisDungeons - Gestor de la Mochila del Abismo
 * Almacena temporalmente el loot de la sesión de los jugadores en la RAM.
 */
@Singleton
public class AbyssBackpackManager {

    private final AeroxisDungeons plugin;
    
    // 🚀 Hilos Virtuales para no bloquear el Main Thread al procesar grandes cantidades de loot
    private final ExecutorService backpackExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    // ⚡ Memoria Concurrente O(1) para máxima velocidad
    private final Map<UUID, List<ItemStack>> activeBackpacks = new ConcurrentHashMap<>();

    @Inject
    public AbyssBackpackManager(AeroxisDungeons plugin) {
        this.plugin = plugin;
    }

    /**
     * Agrega un ítem a la mochila virtual del jugador de forma asíncrona.
     */
    public void addItemAsync(UUID playerId, ItemStack item) {
        backpackExecutor.submit(() -> {
            activeBackpacks.computeIfAbsent(playerId, k -> new ArrayList<>()).add(item.clone());
        });
    }

    /**
     * Obtiene y ELIMINA todos los ítems de la mochila (Usado al Morir o Extraer).
     */
    public List<ItemStack> flushAndGetLoot(UUID playerId) {
        return activeBackpacks.remove(playerId);
    }

    /**
     * Solo lectura de la mochila (Usado para abrir el Menú GUI de visualización).
     */
    public List<ItemStack> viewLoot(UUID playerId) {
        return activeBackpacks.getOrDefault(playerId, new ArrayList<>());
    }
}
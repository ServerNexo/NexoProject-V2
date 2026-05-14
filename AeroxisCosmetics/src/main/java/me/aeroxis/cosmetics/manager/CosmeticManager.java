package me.aeroxis.cosmetics.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎨 AeroxisCosmetics - Gestor de Memoria Thread-Safe (Folia Ready)
 */
@Singleton
public class CosmeticManager {

    private final JavaPlugin plugin;

    // 🌟 AÑADIDO: haloEffect para soportar los nuevos anillos orbitales
    public record ActiveCosmetics(
            String joinTag, String projectileTrail, String killEffect,
            String wingsEffect, String haloEffect, String islandBorder, String islandPet
    ) {}

    // Mapas concurrentes O(1)
    private final Map<UUID, ActiveCosmetics> cosmeticsCache = new ConcurrentHashMap<>();
    private final Map<UUID, ItemDisplay> activePets = new ConcurrentHashMap<>();

    @Inject
    public CosmeticManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ActiveCosmetics getActiveCosmetics(UUID uuid) {
        return cosmeticsCache.get(uuid);
    }

    public void setActiveCosmetics(UUID uuid, ActiveCosmetics cosmetics) {
        cosmeticsCache.put(uuid, cosmetics);
    }

    public void registerPet(UUID uuid, ItemDisplay display) {
        activePets.put(uuid, display);
    }

    public ItemDisplay getPet(UUID uuid) {
        return activePets.get(uuid);
    }

    public void cleanUp(Player player) {
        UUID uuid = player.getUniqueId();
        cosmeticsCache.remove(uuid);

        // 🌟 FIX FOLIA: Eliminar una entidad requiere usar SU propio hilo (EntityScheduler)
        ItemDisplay pet = activePets.remove(uuid);
        if (pet != null && !pet.isDead()) {
            pet.getScheduler().execute(plugin, pet::remove, null, 1L);
        }
    }
}
package me.nexo.islas.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandDatabase;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.instances.IslandSlimeManager; // 🌟 EL NUEVO MOTOR ASP
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏝️ Gestor Central de Islas (Arquitectura ASP V4 + PostgreSQL)
 */
@Singleton
public class IslandManager {

    private final NexoIslas plugin;
    private final IslandDatabase db;
    private final IslandSlimeManager slimeManager;

    // 🌟 CACHÉ EN RAM: UUID de la Isla -> Perfil de la Isla
    private final Map<UUID, IslandProfile> activeIslands = new ConcurrentHashMap<>();

    @Inject
    public IslandManager(NexoIslas plugin, IslandDatabase db, IslandSlimeManager slimeManager) {
        this.plugin = plugin;
        this.db = db;
        this.slimeManager = slimeManager;
    }

    /**
     * Devuelve el Perfil de una isla buscando directamente por el nombre del mundo.
     */
    public IslandProfile getIslandAt(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().startsWith("island_")) return null;

        try {
            // "island_123e4567-e89b-12d3-a456-426614174000" -> UUID
            UUID ownerId = UUID.fromString(loc.getWorld().getName().replace("island_", ""));
            return activeIslands.get(ownerId);
        } catch (IllegalArgumentException e) {
            return null; // El nombre del mundo no era un UUID válido
        }

    }
    /**
     * 🌟 ESPEJO DE PROGRESO: Busca el perfil de la isla directamente por el dueño en la RAM.
     */
    public IslandProfile getIslandByOwner(UUID ownerId) {
        return activeIslands.get(ownerId);
    }

    /**
     * 🌟 CREAR ISLA
     */
    public void createIslandAsync(Player player) {
        player.sendMessage("§e⏳ Contactando a los Arquitectos celestiales...");

        // 1. Registramos al jugador en PostgreSQL
        db.createNewIsland(player.getUniqueId()).thenAccept(result -> {
            // 🌟 FIX: Evaluamos como Integer (-1 es error/ya existe) basado en tu DB actual
            if (result == -1) {
                player.sendMessage("§c❌ Error crítico conectando con el Nexo o ya tienes una isla.");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§a✅ ¡Tus escrituras han sido firmadas!");
                // Llamamos a cargar para que construya el mundo y lo teletransporte
                loadIslandAsync(player);
            });

        }).exceptionally(ex -> {
            plugin.getLogger().severe("❌ Error asíncrono creando isla: " + ex.getMessage());
            return null;
        });
    }

    /**
     * 🌟 CARGAR ISLA (Login o /is)
     */
    public void loadIslandAsync(Player player) {
        player.sendMessage("§e⏳ Desplegando tu isla desde el Vacío...");

        // 1. Cargamos el perfil de la DB
        db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
            if (profile == null) {
                player.sendMessage("§c❌ No tienes una isla. Usa /is create");
                return;
            }

            // 2. Guardamos en RAM
            activeIslands.put(profile.getOwnerId(), profile);

            // 3. Le decimos al Motor Slime que lea el archivo y lo cargue en la RAM de Bukkit
            slimeManager.loadOrGenerateIsland(profile.getOwnerId()).thenAccept(islandWorld -> {
                if (islandWorld != null) {
                    // Teletransportamos al centro de su nuevo micromundo
                    Location islandLoc = new Location(islandWorld, 0, 102, 0); // El centro de ASP V4 suele ser 0,0
                    player.teleportAsync(islandLoc).thenAccept(success -> {
                        if (success) player.sendMessage("§a✅ Volando de regreso a tu isla...");
                    });
                } else {
                    player.sendMessage("§c❌ Error fatal cargando los bloques físicos de la isla.");
                }
            });
        });
    }

    /**
     * 💤 APAGAR ISLA (Hibernación de RAM)
     */
    public void unloadIslandSafe(UUID ownerId) {
        activeIslands.remove(ownerId); // Liberamos Caché
        slimeManager.unloadIsland(ownerId); // Liberamos la RAM del mundo
        plugin.getLogger().info("💤 Isla de " + ownerId + " hibernada (RAM Liberada).");
    }

    public NexoIslas getPlugin() {
        return plugin;
    }
}
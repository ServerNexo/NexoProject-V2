package me.nexo.islas.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandDatabase;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.instances.IslandSlimeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏝️ Gestor Central de Islas (Arquitectura ASP V4 + PostgreSQL)
 * Rendimiento: Guardado Asíncrono, Top Integrado, Caché Concurrente y Físicas.
 */
@Singleton
public class IslandManager {

    private final NexoIslas plugin;
    private final IslandDatabase db;
    private final IslandSlimeManager slimeManager;

    // 🌟 CACHÉ EN RAM: UUID del DUEÑO -> Perfil de la Isla
    private final Map<UUID, IslandProfile> activeIslands = new ConcurrentHashMap<>();

    // 🌟 SESIONES DE RENOMBRE (Bedrock Friendly)
    private final Map<UUID, IslandProfile> renameSessions = new ConcurrentHashMap<>();

    @Inject
    public IslandManager(NexoIslas plugin, IslandDatabase db, IslandSlimeManager slimeManager) {
        this.plugin = plugin;
        this.db = db;
        this.slimeManager = slimeManager;
    }

    public IslandProfile getIslandAt(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().startsWith("island_")) return null;

        try {
            UUID islandId = UUID.fromString(loc.getWorld().getName().replace("island_", ""));
            for (IslandProfile profile : activeIslands.values()) {
                if (profile.getIslandId().equals(islandId)) return profile;
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public IslandProfile getIslandByOwner(UUID ownerId) {
        return activeIslands.get(ownerId);
    }

    // 🌟 NUEVO FIX: Para actualizar la RAM de forma instantánea desde los comandos
    public void cacheIsland(IslandProfile profile) {
        if (profile != null) activeIslands.put(profile.getOwnerId(), profile);
    }

    public CompletableFuture<Void> saveIslandProfileAsync(IslandProfile profile) {
        return CompletableFuture.runAsync(() -> {
            db.saveIslandSync(profile);
        });
    }

    public void generatePhysicalIsland(Player player, IslandProfile newProfile) {
        activeIslands.put(newProfile.getOwnerId(), newProfile);

        slimeManager.loadOrGenerateIsland(newProfile.getIslandId()).thenAccept(islandWorld -> {
            if (islandWorld != null) {
                applyPhysicsAndTeleport(player, newProfile, islandWorld);
            } else {
                player.sendMessage("§c❌ Error fatal generando los bloques físicos de la isla.");
            }
        });
    }

    public void loadIslandAsync(Player player) {
        player.sendMessage("§e⏳ Desplegando tu isla desde el Vacío...");

        db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
            if (profile == null) {
                player.sendMessage("§c❌ No tienes una isla. Usa /is create");
                return;
            }

            activeIslands.put(profile.getOwnerId(), profile);

            slimeManager.loadOrGenerateIsland(profile.getIslandId()).thenAccept(islandWorld -> {
                if (islandWorld != null) {
                    applyPhysicsAndTeleport(player, profile, islandWorld);
                } else {
                    player.sendMessage("§c❌ Error fatal cargando los bloques físicos de la isla.");
                }
            });
        });
    }

    private void applyPhysicsAndTeleport(Player player, IslandProfile profile, org.bukkit.World islandWorld) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                int borderSize = profile.getRealBorderSize();
                org.bukkit.WorldBorder border = islandWorld.getWorldBorder();
                border.setCenter(0, 0);
                border.setSize(borderSize);
                border.setDamageAmount(2.0);
                border.setWarningDistance(5);

                // 🌟 FIX AUDITORÍA: Escáner Inteligente de Altura
                // Esto busca el bloque más alto construido en el centro para no soltarte en el aire.
                int highestY = islandWorld.getHighestBlockYAt(0, 0);
                int spawnY = highestY > 0 ? highestY + 1 : 64; // Fallback seguro por si acaso

                Location islandLoc = new Location(islandWorld, 0.5, spawnY, 0.5);
                player.teleportAsync(islandLoc).thenAccept(success -> {
                    if (success) {
                        player.sendMessage("§a✅ ¡Has llegado a tu dominio!");
                    } else {
                        player.sendMessage("§c❌ El servidor canceló la teletransportación.");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error teletransportando a la isla: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void unloadIslandSafe(IslandProfile profile) {
        if (profile == null) return;

        activeIslands.remove(profile.getOwnerId());

        saveIslandProfileAsync(profile).thenRun(() -> {
            slimeManager.unloadIsland(profile.getIslandId());
            plugin.getLogger().info("💤 Isla " + profile.getIslandId() + " hibernada (RAM Liberada).");
        });
    }

    public void deleteIslandAsync(IslandProfile profile, Player owner) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String worldName = "island_" + profile.getIslandId().toString();
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Location fallbackSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                for (Player p : world.getPlayers()) {
                    p.teleportAsync(fallbackSpawn);
                    p.sendMessage("§c⚠️ La isla en la que estabas ha sido destruida.");
                }
            }

            CompletableFuture.runAsync(() -> {
                activeIslands.remove(profile.getOwnerId());
                slimeManager.unloadIsland(profile.getIslandId());
                db.deleteIslandSync(profile.getOwnerId());
            }).thenRun(() -> {
                owner.sendMessage("§a✅ Tu isla ha sido eliminada para siempre. Ahora puedes crear una nueva.");
            });
        });
    }

    // ==========================================
    // 🛑 PROTOCOLO DE APAGADO SEGURO
    // ==========================================
    public void shutdownSafely() {
        if (activeIslands.isEmpty()) return;

        plugin.getLogger().info("💾 Guardando " + activeIslands.size() + " islas activas en PostgreSQL...");

        for (IslandProfile profile : activeIslands.values()) {
            db.saveIslandSync(profile); // Síncrono para que Spigot no lo cancele al apagar
            slimeManager.unloadIsland(profile.getIslandId());
        }

        activeIslands.clear();
        plugin.getLogger().info("✅ Protocolo de guardado finalizado con éxito.");
    }

    public void addRenameSession(UUID playerId, IslandProfile profile) {
        renameSessions.put(playerId, profile);
    }

    public void removeRenameSession(UUID playerId) {
        renameSessions.remove(playerId);
    }

    public IslandProfile getRenameSession(UUID playerId) {
        return renameSessions.get(playerId);
    }

    public NexoIslas getPlugin() {
        return plugin;
    }
}
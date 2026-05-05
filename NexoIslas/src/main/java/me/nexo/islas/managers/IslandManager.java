package me.nexo.islas.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandDatabase;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.instances.IslandSlimeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    /**
     * Devuelve el Perfil de una isla buscando directamente por el nombre del mundo.
     */
    public IslandProfile getIslandAt(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().startsWith("island_")) return null;

        try {
            UUID islandId = UUID.fromString(loc.getWorld().getName().replace("island_", ""));
            // Buscamos cuál perfil tiene este islandId
            for (IslandProfile profile : activeIslands.values()) {
                if (profile.getIslandId().equals(islandId)) return profile;
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 🌟 ESPEJO DE PROGRESO: Busca el perfil de la isla directamente por el dueño en la RAM.
     */
    public IslandProfile getIslandByOwner(UUID ownerId) {
        return activeIslands.get(ownerId);
    }

    /**
     * 🌟 GUARDADO ASÍNCRONO SEGURO
     */
    public CompletableFuture<Void> saveIslandProfileAsync(IslandProfile profile) {
        return CompletableFuture.runAsync(() -> {
            db.saveIslandSync(profile);
        });
    }

    /**
     * 🌟 GENERAR ISLA FÍSICA Y TELETRANSPORTAR (Llamado desde ComandoIsla)
     */
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

    /**
     * 🌟 CARGAR ISLA (Login o /is home)
     */
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
        // ==========================================
        // 🌟 APLICAR MEJORA DE TAMAÑO FÍSICO AL MUNDO
        // ==========================================
        int borderSize = profile.getRealBorderSize();
        org.bukkit.WorldBorder border = islandWorld.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(borderSize);
        border.setDamageAmount(2.0);
        border.setWarningDistance(5);

        Location islandLoc = new Location(islandWorld, 0.5, 102, 0.5);
        player.teleportAsync(islandLoc).thenAccept(success -> {
            if (success) player.sendMessage("§a✅ ¡Has llegado a tu dominio!");
        });
    }

    /**
     * 💤 APAGAR ISLA (Hibernación de RAM)
     */
    public void unloadIslandSafe(IslandProfile profile) {
        if (profile == null) return;

        activeIslands.remove(profile.getOwnerId());

        saveIslandProfileAsync(profile).thenRun(() -> {
            slimeManager.unloadIsland(profile.getIslandId());
            plugin.getLogger().info("💤 Isla " + profile.getIslandId() + " hibernada (RAM Liberada).");
        });
    }

    // ==========================================
    // 🏷️ GESTIÓN DE SESIONES DE RENOMBRE
    // ==========================================

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
package me.nexo.dungeons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.NexoDungeons;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 🏰 NexoDungeons - Listener de Seguridad y Anti-Dupe (Arquitectura Enterprise)
 */
@Singleton
public class DungeonSecurityListener implements Listener {

    private final NexoDungeons plugin;
    private static final String DUNGEON_WORLD = "nexo_dungeons";

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public DungeonSecurityListener(NexoDungeons plugin) {
        this.plugin = plugin;
    }

    // 🛑 1. ANTI-DUPE: Prevenir que los jugadores tiren ítems clave de la mazmorra al piso
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        Player p = event.getPlayer();

        // Si está en el mundo de instancias y no es admin en creativo...
        if (p.getWorld().getName().equals(DUNGEON_WORLD) && p.getGameMode() != GameMode.CREATIVE) {
            // Cancelamos que tiren basura o ítems de la mazmorra al suelo para evitar exploits
            event.setCancelled(true);
            CrossplayUtils.sendMessage(p, "&#FF5555[!] La magia de la mazmorra te impide arrojar objetos aquí.");
        }
    }

    // 🛑 2. ANTI-DUPE: Cancelar transacciones raras al cerrar inventarios
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer().getWorld().getName().equals(DUNGEON_WORLD)) {
            // Actualizamos el inventario del jugador un tick después de cerrar el menú.
            // Esto elimina instantáneamente cualquier "Ghost Item" que se haya quedado pegado en el cursor
            // por culpa de un cliente modificado o mal lag de red.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer() instanceof Player p && p.isOnline()) {
                    p.updateInventory();
                }
            }, 1L);
        }
    }

    // 🚪 3. CONTROL DE SESIÓN: Evitar que queden atrapados en el Vacío
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (p.getWorld().getName().equals(DUNGEON_WORLD)) {
            // 🌟 FIX: Teletransporte Asíncrono de Paper *antes* de que el jugador sea removido del servidor.
            // JAMÁS usar un runTaskLater aquí, o crasheará al intentar mover a un jugador offline.
            p.teleportAsync(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }

    // 🛡️ CAPA DE SEGURIDAD EXTRA: Si el servidor crashea y guardó al jugador dentro de la dungeon
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (p.getWorld().getName().equals(DUNGEON_WORLD)) {
            p.teleportAsync(Bukkit.getWorlds().get(0).getSpawnLocation()).thenAccept(success -> {
                if (success) {
                    CrossplayUtils.sendMessage(p, "&#FFAA00[!] Has sido evacuado a una zona segura tras el colapso de la mazmorra.");
                }
            });
        }
    }
}
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
 * Rendimiento: PlayerScheduler Folia-Ready, Inyección Segura y Cero Estáticos.
 */
@Singleton
public class DungeonSecurityListener implements Listener {

    private final NexoDungeons plugin;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia Inyectada

    private static final String DUNGEON_WORLD = "nexo_dungeons";

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public DungeonSecurityListener(NexoDungeons plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    // 🛑 1. ANTI-DUPE: Prevenir que los jugadores tiren ítems clave de la mazmorra al piso
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        var p = event.getPlayer();

        // Si está en el mundo de instancias y no es admin en creativo...
        if (p.getWorld().getName().equals(DUNGEON_WORLD) && p.getGameMode() != GameMode.CREATIVE) {
            // Cancelamos que tiren basura o ítems de la mazmorra al suelo para evitar exploits
            event.setCancelled(true);
            crossplayUtils.sendMessage(p, "&#FF5555[!] La magia de la mazmorra te impide arrojar objetos aquí.");
        }
    }

    // 🛑 2. ANTI-DUPE: Cancelar transacciones raras al cerrar inventarios
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        var p = (Player) event.getPlayer();
        
        if (p.getWorld().getName().equals(DUNGEON_WORLD)) {
            // 🌟 FOLIA NATIVE: Usamos PlayerScheduler atado al jugador en lugar del scheduler global.
            // Esto elimina el "Ghost Item" instantáneamente en el mismo hilo de red del cliente.
            p.getScheduler().runDelayed(plugin, task -> {
                if (p.isOnline()) {
                    p.updateInventory();
                }
            }, null, 1L);
        }
    }

    // 🚪 3. CONTROL DE SESIÓN: Evitar que queden atrapados en el Vacío
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var p = event.getPlayer();
        
        if (p.getWorld().getName().equals(DUNGEON_WORLD)) {
            // 🌟 FIX: Validación de seguridad para el mundo por defecto
            var defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (defaultWorld != null) {
                p.teleportAsync(defaultWorld.getSpawnLocation());
            }
        }
    }

    // 🛡️ CAPA DE SEGURIDAD EXTRA: Si el servidor crashea y guardó al jugador dentro de la dungeon
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var p = event.getPlayer();
        
        if (p.getWorld().getName().equals(DUNGEON_WORLD)) {
            var defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (defaultWorld != null) {
                p.teleportAsync(defaultWorld.getSpawnLocation()).thenAccept(success -> {
                    // Validar si el jugador sigue en línea tras el teletransporte asíncrono
                    if (success && p.isOnline()) {
                        crossplayUtils.sendMessage(p, "&#FFAA00[!] Has sido evacuado a una zona segura tras el colapso de la mazmorra.");
                    }
                });
            }
        }
    }
}
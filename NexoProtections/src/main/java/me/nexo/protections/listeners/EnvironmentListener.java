package me.nexo.protections.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ClaimAction;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 🛡️ NexoProtections - Listener de Entorno (Arquitectura Enterprise)
 * Rendimiento: Evaluaciones Zero-Garbage (O(1)), Prevención de pisotones de cultivos y Dependencias Inyectadas.
 */
@Singleton
public class EnvironmentListener implements Listener {

    private final ClaimManager claimManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    // 💉 PILAR 1: Inyección Pura y Desacoplada
    @Inject
    public EnvironmentListener(ClaimManager claimManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.claimManager = claimManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    // =========================================================================
    // 💥 PROTECCIÓN CONTRA EXPLOSIONES (TNT, CREEPERS, WITHER)
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // 🚀 Gracias a nuestro nuevo Spatial Grid O(1), este removeIf se ejecutará
        // en microsegundos sin importar si la explosión afecta 500 bloques.
        event.blockList().removeIf(block -> claimManager.getStoneAt(block.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> claimManager.getStoneAt(block.getLocation()) != null);
    }

    // =========================================================================
    // ✋ PROTECCIÓN DE INTERACCIONES Y CONTENEDORES
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ignoramos la mano secundaria para evitar doble-ejecución
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        var block = event.getClickedBlock();
        if (block == null) return;

        // No bloqueamos los clics izquierdos al aire o a monstruos
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) return;

        var player = event.getPlayer();
        var stone = claimManager.getStoneAt(block.getLocation());

        if (stone == null) return; // Fuera de dominio, permitido

        // Bypass de dueños, permisos de clan y administradores
        if (stone.getOwnerId().equals(player.getUniqueId()) ||
                stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT) ||
                player.hasPermission("nexoprotections.admin")) {
            return;
        }

        // 🌾 FIX CRÍTICO: Bloqueo de pisotones a cultivos y placas de presión
        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true);
            return;
        }

        String name = block.getType().name(); // 🌟 OPTIMIZACIÓN: .name() NO crea basura en RAM. Devuelve la constante Enum.

        // 1. LEY DE CONTENEDORES (Cofres, Hornos, Shulkers, etc)
        boolean isContainer = name.contains("CHEST") || name.contains("SHULKER") || name.contains("FURNACE") ||
                name.equals("BARREL") || name.equals("HOPPER") || name.equals("DROPPER") ||
                name.equals("DISPENSER") || name.equals("SMOKER");

        if (isContainer) {
            if (!stone.getFlag("containers")) {
                event.setCancelled(true);
                crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().tesorosSellados());
                return;
            }
        }

        // 2. LEY DE INTERACCIÓN GENERAL (Puertas, Botones, Palancas, Yunques)
        boolean isInteractable = name.contains("DOOR") || name.contains("BUTTON") || name.equals("LEVER") ||
                name.contains("ANVIL") || name.equals("CRAFTING_TABLE") || name.equals("LOOM") ||
                name.contains("BED") || name.equals("BELL");

        if (isInteractable) {
            if (!stone.getFlag("interact")) {
                event.setCancelled(true);
                crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinInteractuar());
            }
        }
    }

    // =========================================================================
    // 🗑️ PROTECCIÓN CONTRA BASURA (ITEM DROPS)
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        var player = event.getPlayer();
        var stone = claimManager.getStoneAt(player.getLocation());

        if (stone == null) return;

        if (stone.getOwnerId().equals(player.getUniqueId()) ||
                stone.hasPermission(player.getUniqueId(), ClaimAction.INTERACT) ||
                player.hasPermission("nexoprotections.admin")) {
            return;
        }

        // Si la ley "item-drop" está en false, bloqueamos que ensucien la base
        if (!stone.getFlag("item-drop")) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().noArrojarOfrendas());
        }
    }
}
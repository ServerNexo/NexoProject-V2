package me.nexo.minions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.manager.MinionManager;
import me.nexo.minions.menu.MinionMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * 🤖 NexoMinions - Listener de Interacción (Arquitectura Enterprise)
 * Rendimiento: Llavescacheadas O(1), Prevención de Doble-Disparo y Cero objetos basura.
 */
@Singleton
public class MinionInteractListener implements Listener {

    private final NexoMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;

    // 🌟 OPTIMIZACIÓN: Cacheamos la llave para no instanciarla en cada clic
    private final NamespacedKey interactionKey;

    @Inject
    public MinionInteractListener(NexoMinions plugin, MinionManager minionManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;

        this.interactionKey = new NamespacedKey(plugin, "minion_display_id");
    }

    // ==========================================
    // 🖱️ CLIC DERECHO: ABRIR MENÚ
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEntityEvent event) {
        // 🌟 FIX CRÍTICO: Previene el "Double Fire Bug" de Bukkit (MainHand y OffHand)
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (!(event.getRightClicked() instanceof Interaction hitbox)) return;

        // Búsqueda rápida con llave cacheada
        String displayIdStr = hitbox.getPersistentDataContainer().get(interactionKey, PersistentDataType.STRING);
        if (displayIdStr == null) return;

        // Cancelamos tempranamente para evitar que el jugador interactúe con armaduras o bloques detrás
        event.setCancelled(true);

        UUID displayId;
        try {
            displayId = UUID.fromString(displayIdStr);
        } catch (IllegalArgumentException e) {
            return; // PDC Corrupto
        }

        ActiveMinion minion = minionManager.getMinion(displayId);
        if (minion == null) return;

        Player player = event.getPlayer();

        // 🛡️ PARCHE DE SEGURIDAD
        if (!minion.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().manager().interactuarAjeno());
            return;
        }

        // Abrimos la interfaz holográfica
        new MinionMenu(player, plugin, minion).open();
    }

    // ==========================================
    // 🗡️ CLIC IZQUIERDO: RECOGER MINION
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction hitbox)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        String displayIdStr = hitbox.getPersistentDataContainer().get(interactionKey, PersistentDataType.STRING);
        if (displayIdStr == null) return;

        // Cancelamos el daño para que la entidad de interacción no sufra retroceso ni efectos
        event.setCancelled(true);

        UUID displayId;
        try {
            displayId = UUID.fromString(displayIdStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        ActiveMinion minion = minionManager.getMinion(displayId);
        if (minion == null) return;

        // 🛡️ PARCHE DE SEGURIDAD
        if (!minion.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().manager().desterrarAjeno());
            return;
        }

        // Extracción remota y desmantelamiento
        minionManager.recogerMinion(player, displayId);
    }
}
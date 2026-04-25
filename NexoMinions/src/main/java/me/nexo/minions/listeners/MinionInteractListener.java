package me.nexo.minions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.TiersConfig;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.manager.MinionManager;
import me.nexo.minions.menu.MinionMenu;
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
 * Rendimiento: Llaves cacheadas O(1), Prevención de Doble-Disparo y Lectura de Genoma.
 */
@Singleton
public class MinionInteractListener implements Listener {

    private final NexoMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;

    // 🌟 Sinergias propagadas exclusivamente para construir el menú de forma limpia
    private final TiersConfig tiersConfig;
    private final UpgradesConfig upgradesConfig;
    private final CrossplayUtils crossplayUtils;
    private final CollectionManager collectionManager;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public MinionInteractListener(NexoMinions plugin, MinionManager minionManager, ConfigManager configManager,
                                  TiersConfig tiersConfig, UpgradesConfig upgradesConfig,
                                  CrossplayUtils crossplayUtils, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;
        this.tiersConfig = tiersConfig;
        this.upgradesConfig = upgradesConfig;
        this.crossplayUtils = crossplayUtils;
        this.collectionManager = collectionManager;
    }

    // ==========================================
    // 🖱️ CLIC DERECHO: ABRIR MENÚ O NEGOCIAR HUELGA
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEntityEvent event) {
        // FIX CRÍTICO: Previene el "Double Fire Bug" de Bukkit (MainHand y OffHand)
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (!(event.getRightClicked() instanceof Interaction hitbox)) return;

        // Búsqueda rápida con llave centralizada O(1)
        String displayIdStr = hitbox.getPersistentDataContainer().get(MinionKeys.INTERACTION_ID, PersistentDataType.STRING);
        if (displayIdStr == null) return;

        // Cancelamos tempranamente para evitar que el jugador interactúe con armaduras o bloques detrás
        event.setCancelled(true);

        UUID displayId;
        try {
            displayId = UUID.fromString(displayIdStr);
        } catch (IllegalArgumentException e) {
            return; // PDC Corrupto
        }

        var minion = minionManager.getMinion(displayId);
        if (minion == null) return;

        var player = event.getPlayer();

        // 🧬 LECTURA DEL GENOMA: Obtenemos el ownerId a través del ADN inyectado
        if (!minion.getDna().ownerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
            crossplayUtils.sendMessage(player, configManager.getMessages().manager().interactuarAjeno());
            return;
        }

        // 🚦 MECÁNICA FASE 3: Negociación de Huelga
        if (minion.getState() == ActiveMinion.MinionState.ON_STRIKE) {
            minion.cureFatigue();
            crossplayUtils.sendMessage(player, "&#55FF55[✓] Has negociado con el Sindicato. ¡El Minion ha vuelto al trabajo!");
            return; // Detenemos aquí para que la interacción sirva como "cura" antes de abrir el menú de nuevo
        }

        // 🌟 APERTURA DE INTERFAZ
        new MinionMenu(player, minion, plugin, configManager, tiersConfig, upgradesConfig, minionManager, crossplayUtils, collectionManager).open();
    }

    // ==========================================
    // 🗡️ CLIC IZQUIERDO: RECOGER MINION
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction hitbox)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        String displayIdStr = hitbox.getPersistentDataContainer().get(MinionKeys.INTERACTION_ID, PersistentDataType.STRING);
        if (displayIdStr == null) return;

        // Cancelamos el daño para que la entidad de interacción no sufra retroceso ni efectos
        event.setCancelled(true);

        UUID displayId;
        try {
            displayId = UUID.fromString(displayIdStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        var minion = minionManager.getMinion(displayId);
        if (minion == null) return;

        // 🧬 LECTURA DEL GENOMA: Obtenemos el ownerId a través del ADN inyectado
        if (!minion.getDna().ownerId().equals(player.getUniqueId()) && !player.hasPermission("nexominions.admin")) {
            crossplayUtils.sendMessage(player, configManager.getMessages().manager().desterrarAjeno());
            return;
        }

        // Extracción remota y desmantelamiento
        minionManager.recogerMinion(player, displayId);
    }
}
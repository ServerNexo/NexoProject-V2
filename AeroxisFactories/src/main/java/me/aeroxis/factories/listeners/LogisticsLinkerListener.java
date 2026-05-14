package me.aeroxis.factories.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.factories.core.ActiveFactory;
import me.aeroxis.factories.managers.FactoryManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔗 Enlazador Logístico Omni-Direccional
 * Conecta Fábricas y Omni-Minions usando Arquitectura Orientada a Eventos.
 */
@Singleton
public class LogisticsLinkerListener implements Listener {

    private final FactoryManager factoryManager;
    private final CrossplayUtils crossplayUtils;

    // Memoria temporal: UUID del Jugador -> UUID del Nodo Origen
    private final Map<UUID, UUID> pendingLinks = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingTypes = new ConcurrentHashMap<>();

    // ==========================================
    // 📡 EVENTO CUSTOM PARA COMUNICACIÓN CROSS-PLUGIN
    // ==========================================
    public static class MinionLinkedEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final UUID minionId;
        private final UUID factoryId;

        public MinionLinkedEvent(UUID minionId, UUID factoryId) {
            this.minionId = minionId;
            this.factoryId = factoryId;
        }

        public UUID getMinionId() { return minionId; }
        public UUID getFactoryId() { return factoryId; }

        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    @Inject
    public LogisticsLinkerListener(FactoryManager factoryManager, CrossplayUtils crossplayUtils) {
        this.factoryManager = factoryManager;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🤖 CLIC EN ENTIDAD (OMNI-MINION)
    // ==========================================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMinionLinkInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        // 🌟 CONDICIÓN FASE 4: Shift + Clic
        if (!event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.isEmpty()) return;

        /* * 🌟 NOTA DEL ARQUITECTO:
         * Asegúrate de crear un ítem en la configuración de tu plugin Nexo llamado 'logistics_linker'
         * con la textura de una llave inglesa, control remoto o cable.
         */
        String nexoId = com.nexomc.nexo.api.NexoItems.idFromItem(item);
        if (nexoId == null || !nexoId.equals("logistics_linker")) return;

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Interaction)) return;

        // Verificamos si es un Minion leyendo el PDC de Bukkit
        NamespacedKey interactionKey = new NamespacedKey("nexominions", "interaction_id");
        String displayIdStr = clicked.getPersistentDataContainer().get(interactionKey, PersistentDataType.STRING);
        if (displayIdStr == null) return;

        event.setCancelled(true);

        UUID minionId = UUID.fromString(displayIdStr);
        pendingLinks.put(player.getUniqueId(), minionId);
        pendingTypes.put(player.getUniqueId(), "MINION");

        crossplayUtils.sendMessage(player, "&#00f5ff[🔗] <bold>MINION ORIGEN MARCADO:</bold> &#E6CCFFAhora golpea el núcleo de la fábrica destino o usa /silo link.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1.5f);
    }

    // ==========================================
    // 🏭 CLIC EN BLOQUE (FÁBRICA DESTINO/ORIGEN)
    // ==========================================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onFactoryLinkInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (item == null || item.isEmpty()) return;

        /* * 🌟 NOTA DEL ARQUITECTO:
         * Asegúrate de crear un ítem en la configuración de tu plugin Nexo llamado 'logistics_linker'
         * con la textura de una llave inglesa, control remoto o cable.
         */
        String nexoId = com.nexomc.nexo.api.NexoItems.idFromItem(item);
        if (nexoId == null || !nexoId.equals("logistics_linker")) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        ActiveFactory clickedFactory = factoryManager.getFactoryAt(clicked.getLocation());
        if (clickedFactory == null) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        UUID originId = pendingLinks.get(player.getUniqueId());
        String originType = pendingTypes.getOrDefault(player.getUniqueId(), "FACTORY");

        if (originId == null) {
            // 📍 CLIC 1: Marcar Origen (Fábrica)
            if (!clickedFactory.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexofactories.admin")) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Solo el propietario puede reconfigurar la red logística.");
                return;
            }

            pendingLinks.put(player.getUniqueId(), clickedFactory.getId());
            pendingTypes.put(player.getUniqueId(), "FACTORY");
            crossplayUtils.sendMessage(player, "&#00f5ff[🔗] <bold>FÁBRICA ORIGEN MARCADA:</bold> &#E6CCFFAhora golpea el núcleo de la fábrica destino o usa /silo link.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1.5f);
        } else {
            // 📍 CLIC 2: Establecer Destino Físico
            if (originId.equals(clickedFactory.getId())) {
                crossplayUtils.sendMessage(player, "&#FF5555[x] Bucle temporal detectado. No puedes enlazar un nodo consigo mismo.");
                pendingLinks.remove(player.getUniqueId());
                pendingTypes.remove(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (originType.equals("MINION")) {
                // 🚀 Disparamos el evento para que NexoMinions lo atrape
                org.bukkit.Bukkit.getPluginManager().callEvent(new MinionLinkedEvent(originId, clickedFactory.getId()));

                crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>¡RED ESTABLECIDA!</bold> &#E6CCFFEl Omni-Minion ahora inyectará sus recursos en esta Fábrica.");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            } else {
                // Enlace Fábrica -> Fábrica
                ActiveFactory originFactory = factoryManager.getFactoryById(originId);
                if (originFactory != null) {
                    originFactory.setTargetLinkId(clickedFactory.getId());
                    factoryManager.saveFactoryStatusAsync(originFactory);
                    crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>¡RED ESTABLECIDA!</bold> &#E6CCFFLos recursos fluirán asíncronamente al destino.");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                }
            }

            pendingLinks.remove(player.getUniqueId());
            pendingTypes.remove(player.getUniqueId());
        }
    }

    // ==========================================
    // ☁️ ENLACE A LA NUBE (COMANDO VIP)
    // ==========================================
    public void linkToCloud(Player player) {
        UUID originId = pendingLinks.get(player.getUniqueId());
        String originType = pendingTypes.getOrDefault(player.getUniqueId(), "FACTORY");

        if (originId == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Primero debes marcar una Fábrica o Minion origen usando tu Logistics Linker.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (originType.equals("MINION")) {
            // 🚀 Disparamos el evento para que NexoMinions lo asigne al UUID del jugador
            org.bukkit.Bukkit.getPluginManager().callEvent(new MinionLinkedEvent(originId, player.getUniqueId()));
            crossplayUtils.sendMessage(player, "&#55FF55[☁] <bold>¡NEXO ESTABLECIDO!</bold> &#E6CCFFEl Minion inyectará sus recursos directamente en tu Silo Virtual.");
        } else {
            // Es una fábrica
            ActiveFactory originFactory = factoryManager.getFactoryById(originId);
            if (originFactory != null) {
                originFactory.setTargetLinkId(player.getUniqueId()); // El destino es el jugador (La Nube)
                factoryManager.saveFactoryStatusAsync(originFactory);
                crossplayUtils.sendMessage(player, "&#55FF55[☁] <bold>¡NEXO ESTABLECIDO!</bold> &#E6CCFFLa Fábrica inyectará sus recursos directamente en tu Silo Virtual.");
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);

        // Limpiamos la memoria
        pendingLinks.remove(player.getUniqueId());
        pendingTypes.remove(player.getUniqueId());
    }
}
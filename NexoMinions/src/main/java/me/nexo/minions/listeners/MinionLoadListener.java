package me.nexo.minions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * 🤖 NexoMinions - Listener de Carga de Chunks (Arquitectura Enterprise)
 * Rendimiento: Llaves Cacheadas (Zero-Garbage), Singleton Estricto y Dependencias Propagadas.
 */
@Singleton
public class MinionLoadListener implements Listener {

    private final NexoMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;

    // 🌟 Sinergias propagadas para la instanciación de ActiveMinion
    private final UpgradesConfig upgradesConfig;
    private final CrossplayUtils crossplayUtils;
    private final CollectionManager collectionManager;

    // 🌟 OPTIMIZACIÓN RAM: Llaves cacheadas para evitar instanciarlas miles de veces por chunk
    private final NamespacedKey holoKey;
    private final NamespacedKey interactionKey;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public MinionLoadListener(NexoMinions plugin, MinionManager minionManager, ConfigManager configManager,
                              UpgradesConfig upgradesConfig, CrossplayUtils crossplayUtils, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;
        this.upgradesConfig = upgradesConfig;
        this.crossplayUtils = crossplayUtils;
        this.collectionManager = collectionManager;

        // Caché de llaves
        this.holoKey = new NamespacedKey(plugin, "minion_holo_id");
        this.interactionKey = new NamespacedKey(plugin, "minion_display_id");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        long currentTime = System.currentTimeMillis();

        // Escaneamos las entidades del chunk que acaba de cargar
        for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof ItemDisplay display)) continue;

            // Revisamos si es un Minion (Si tiene dueño en su PDC)
            var pdc = display.getPersistentDataContainer();
            if (!pdc.has(MinionKeys.OWNER, PersistentDataType.STRING)) continue;

            // Si ya está en la memoria RAM del Manager, lo ignoramos
            if (minionManager.getMinion(display.getUniqueId()) != null) continue;

            // ¡Es un Minion huérfano! Lo leemos y lo reconectamos
            UUID ownerId = UUID.fromString(pdc.get(MinionKeys.OWNER, PersistentDataType.STRING));
            MinionType type = MinionType.valueOf(pdc.get(MinionKeys.TYPE, PersistentDataType.STRING));
            int tier = pdc.getOrDefault(MinionKeys.TIER, PersistentDataType.INTEGER, 1);
            long nextAction = pdc.getOrDefault(MinionKeys.NEXT_ACTION, PersistentDataType.LONG, currentTime);
            int stored = pdc.getOrDefault(MinionKeys.STORED_ITEMS, PersistentDataType.INTEGER, 0);

            // 1. Buscamos su caja de colisiones (Hitbox) cercana
            Interaction hitbox = null;
            for (Entity nearby : display.getNearbyEntities(0.1, 0.1, 0.1)) {
                if (nearby instanceof Interaction inter) {
                    // Usamos la llave cacheada O(1)
                    String linkedId = inter.getPersistentDataContainer().get(interactionKey, PersistentDataType.STRING);
                    if (linkedId != null && linkedId.equals(display.getUniqueId().toString())) {
                        hitbox = inter;
                        break;
                    }
                }
            }

            // 2. Buscamos su Holograma flotante en el chunk
            TextDisplay holograma = null;
            String holoIdStr = pdc.get(holoKey, PersistentDataType.STRING);

            if (holoIdStr != null) {
                UUID holoId = UUID.fromString(holoIdStr);
                for (Entity e : event.getChunk().getEntities()) {
                    if (e instanceof TextDisplay td && td.getUniqueId().equals(holoId)) {
                        holograma = td;
                        break;
                    }
                }
            }

            // 3. SISTEMA ANTI-ERRORES: Si el holograma se borró por error, lo recreamos en el Hilo Principal
            if (holograma == null) {
                Location holoLoc = display.getLocation().clone().add(0, 1.2, 0);
                holograma = display.getWorld().spawn(holoLoc, TextDisplay.class, holo -> {
                    holo.setBillboard(TextDisplay.Billboard.CENTER);
                    holo.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0));

                    // 🌟 FIX: Parseo inyectado en lugar de estático
                    holo.text(crossplayUtils.parseCrossplay(null, configManager.getMessages().manager().iniciandoSistemas()));
                });
                // Actualizamos la ID del nuevo holograma en el Minion
                pdc.set(holoKey, PersistentDataType.STRING, holograma.getUniqueId().toString());
            }

            // 4. Recreamos al Operario con el nuevo constructor inyectado
            var minion = new ActiveMinion(
                    plugin, display, hitbox, holograma, ownerId, type, tier, nextAction, stored,
                    upgradesConfig, minionManager, crossplayUtils, collectionManager
            );

            // 5. LA MAGIA: Calculamos el trabajo mientras el servidor/chunk estaba apagado
            // 🌟 FIX: DESCOMENTADO, el método ya existe en la clase ActiveMinion que purificamos
            minion.calcularTrabajoOffline(currentTime);

            // Lo metemos de vuelta a la memoria RAM de alta velocidad
            minionManager.getMinionsActivos().put(display.getUniqueId(), minion);
        }
    }
}
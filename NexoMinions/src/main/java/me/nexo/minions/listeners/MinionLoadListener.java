package me.nexo.minions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionDNA;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.UpgradesConfig;
import me.nexo.minions.manager.ActiveMinion;
import me.nexo.minions.manager.MinionManager;
import org.bukkit.Location;
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
 * Rendimiento: Decodificación Binaria O(1), Singleton Estricto y Cálculo Offline.
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
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        long currentTime = System.currentTimeMillis();

        // Escaneamos las entidades del chunk que acaba de cargar
        for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof ItemDisplay display)) continue;

            var pdc = display.getPersistentDataContainer();

            // 🌟 FASE 3: Revisamos si tiene nuestro Genoma Binario Custom
            if (!pdc.has(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE)) continue;

            // Si ya está en la memoria RAM del Manager, lo ignoramos
            if (minionManager.getMinion(display.getUniqueId()) != null) continue;

            // 🧬 LECTURA ULTRA-RÁPIDA: Deserializamos el ADN en un solo paso
            MinionDNA dna = pdc.get(MinionKeys.DNA_KEY, MinionKeys.DNA_TYPE);
            if (dna == null) continue;

            // 1. Buscamos su caja de colisiones (Hitbox) cercana
            Interaction hitbox = null;
            for (Entity nearby : display.getNearbyEntities(0.1, 0.1, 0.1)) {
                if (nearby instanceof Interaction inter) {
                    // Usamos la llave cacheada O(1)
                    String linkedId = inter.getPersistentDataContainer().get(MinionKeys.INTERACTION_ID, PersistentDataType.STRING);
                    if (linkedId != null && linkedId.equals(display.getUniqueId().toString())) {
                        hitbox = inter;
                        break;
                    }
                }
            }

            // 2. Buscamos su Holograma flotante en el chunk
            TextDisplay holograma = null;
            String holoIdStr = pdc.get(MinionKeys.HOLO_ID, PersistentDataType.STRING);

            if (holoIdStr != null) {
                UUID holoId = UUID.fromString(holoIdStr);
                for (Entity e : event.getChunk().getEntities()) {
                    if (e instanceof TextDisplay td && td.getUniqueId().equals(holoId)) {
                        holograma = td;
                        break;
                    }
                }
            }

            // 3. SISTEMA ANTI-ERRORES: Si el holograma se borró por error, lo recreamos
            if (holograma == null) {
                Location holoLoc = display.getLocation().clone().add(0, 1.2, 0);
                holograma = display.getWorld().spawn(holoLoc, TextDisplay.class, holo -> {
                    holo.setBillboard(TextDisplay.Billboard.CENTER);
                    holo.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0));
                    holo.text(crossplayUtils.parseCrossplay(null, "&#55FF55[⚙] Restaurando Sistemas..."));
                });
                // Actualizamos la ID del nuevo holograma en el Minion
                pdc.set(MinionKeys.HOLO_ID, PersistentDataType.STRING, holograma.getUniqueId().toString());
            }

            // 4. Recreamos al Operario con el nuevo constructor inyectado (Pasando el ADN)
            var minion = new ActiveMinion(
                    plugin, display, hitbox, holograma, dna,
                    upgradesConfig, minionManager, crossplayUtils, collectionManager
            );

            // 5. LA MAGIA: Calculamos todo lo que minó mientras el chunk no existía
            minion.calcularTrabajoOffline(currentTime);

            // Lo metemos de vuelta a la memoria RAM de alta velocidad
            minionManager.getMinionsActivos().put(display.getUniqueId(), minion);
        }
    }
}
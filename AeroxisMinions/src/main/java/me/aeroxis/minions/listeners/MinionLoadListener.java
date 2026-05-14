package me.aeroxis.minions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.colecciones.colecciones.CollectionManager;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.islas.managers.IslandLevelEngine; // 🌟 IMPORT DEL MOTOR DE NIVELES
import me.aeroxis.islas.managers.IslandManager;
import me.aeroxis.minions.AeroxisMinions;
import me.aeroxis.minions.config.ConfigManager;
import me.aeroxis.minions.data.MinionDNA;
import me.aeroxis.minions.data.MinionKeys;
import me.aeroxis.minions.data.UpgradesConfig;
import me.aeroxis.minions.manager.ActiveMinion;
import me.aeroxis.minions.manager.MinionManager;
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
 * 🤖 AeroxisMinions - Listener de Carga de Chunks (Arquitectura Enterprise)
 * Rendimiento: Decodificación Binaria O(1), Cálculo Offline y Progreso de Islas.
 */
@Singleton
public class MinionLoadListener implements Listener {

    private final AeroxisMinions plugin;
    private final MinionManager minionManager;
    private final ConfigManager configManager;

    // 🌟 Sinergias propagadas para la instanciación de ActiveMinion
    private final UpgradesConfig upgradesConfig;
    private final CrossplayUtils crossplayUtils;
    private final CollectionManager collectionManager;
    private final IslandManager islandManager;
    private final IslandLevelEngine islandLevelEngine; // 🌟 AÑADIDO: Motor de niveles

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public MinionLoadListener(AeroxisMinions plugin, MinionManager minionManager, ConfigManager configManager,
                              UpgradesConfig upgradesConfig, CrossplayUtils crossplayUtils,
                              CollectionManager collectionManager, IslandManager islandManager,
                              IslandLevelEngine islandLevelEngine) { // 🌟 INYECTADO AQUÍ
        this.plugin = plugin;
        this.minionManager = minionManager;
        this.configManager = configManager;
        this.upgradesConfig = upgradesConfig;
        this.crossplayUtils = crossplayUtils;
        this.collectionManager = collectionManager;
        this.islandManager = islandManager;
        this.islandLevelEngine = islandLevelEngine; // 🌟 GUARDADO
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

            // 4. Recreamos al Operario con el nuevo constructor inyectado
            var minion = new ActiveMinion(
                    plugin, display, hitbox, holograma, dna,
                    upgradesConfig, minionManager, crossplayUtils, collectionManager,
                    islandManager, islandLevelEngine // 🌟 FIX: PASAMOS EL MOTOR AL FINAL
            );

            // 5. LA MAGIA: Calculamos todo lo que minó mientras el chunk no existía (Offline calculation)
            minion.calcularTrabajoOffline(currentTime);

            // Lo metemos de vuelta a la memoria RAM de alta velocidad
            minionManager.getMinionsActivos().put(display.getUniqueId(), minion);
        }
    }
}
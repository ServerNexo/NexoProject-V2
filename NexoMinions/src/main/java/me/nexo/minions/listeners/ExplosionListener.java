package me.nexo.minions.listeners;

import com.google.inject.Inject;
import me.nexo.minions.NexoMinions;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 🤖 NexoMinions - Listener de Explosiones (Arquitectura Enterprise)
 */
public class ExplosionListener implements Listener {

    private final NexoMinions plugin;
    private final NamespacedKey displayIdKey;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ExplosionListener(NexoMinions plugin) {
        this.plugin = plugin;
        this.displayIdKey = new NamespacedKey(plugin, "minion_display_id"); // 💡 Cacheamos la Key
    }

    // 💥 Cubre explosiones de entidades (Creepers, TNT, Wither, Fireballs)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protegerSueloMinions(event.blockList());
    }

    // 💥 Cubre explosiones de bloques (Camas en el Nether, Respawn Anchors)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        protegerSueloMinions(event.blockList());
    }

    private void protegerSueloMinions(List<Block> bloquesDestruidos) {
        // 💡 Código modernizado y ultra rápido: Usamos removeIf nativo de Java
        bloquesDestruidos.removeIf(block -> {
            // Calculamos el espacio justo encima del bloque que va a explotar
            Location topLoc = block.getLocation().add(0.5, 1.0, 0.5);

            // Escaneamos si hay un Minion encima de este bloque
            for (Entity entity : topLoc.getWorld().getNearbyEntities(topLoc, 0.5, 0.5, 0.5)) {
                if (entity instanceof Interaction hitbox) {
                    if (hitbox.getPersistentDataContainer().has(displayIdKey, PersistentDataType.STRING)) {
                        return true; // ¡Hay un Minion aquí! Removemos este bloque de la explosión
                    }
                }
            }
            return false; // El bloque no tiene Minions, puede explotar
        });
    }
}
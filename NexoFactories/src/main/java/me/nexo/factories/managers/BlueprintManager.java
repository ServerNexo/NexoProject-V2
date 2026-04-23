package me.nexo.factories.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.core.StructureTemplate;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏭 NexoFactories - Proyector de Hologramas de Construcción (Arquitectura Enterprise Java 21)
 * Rendimiento: Paper Native Entity Spawning, Inyección Estricta y Cero God Objects.
 */
@Singleton
public class BlueprintManager implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final FactoryManager factoryManager;
    private final CrossplayUtils crossplayUtils;
    private final ClaimManager claimManager;

    private final Map<UUID, List<BlockDisplay>> activeHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Location> activeCores = new ConcurrentHashMap<>();
    private final Map<UUID, StructureTemplate> activeTemplates = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa y Estricta
    @Inject
    public BlueprintManager(FactoryManager factoryManager, CrossplayUtils crossplayUtils, ClaimManager claimManager) {
        this.factoryManager = factoryManager;
        this.crossplayUtils = crossplayUtils;
        this.claimManager = claimManager;
    }

    public void projectBlueprint(Player player, Location coreLocation, StructureTemplate template) {
        clearBlueprint(player);
        List<BlockDisplay> displays = new ArrayList<>();

        for (Map.Entry<Vector, Material> entry : template.getRequiredBlocks().entrySet()) {
            Vector rel = entry.getKey();
            Material mat = entry.getValue();
            Location displayLoc = coreLocation.clone().add(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ());

            if (displayLoc.getBlock().getType() == mat) continue;

            // 🌟 PAPER NATIVE: Spawning atómico (Evita el flickering visual del cliente)
            var display = coreLocation.getWorld().spawn(displayLoc, BlockDisplay.class, d -> {
                d.setBlock(Bukkit.createBlockData(mat));
                d.setTransformation(new Transformation(
                        new org.joml.Vector3f(0.2f, 0.2f, 0.2f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(0.6f, 0.6f, 0.6f),
                        new org.joml.Quaternionf()
                ));
                d.setGlowing(true);
            });
            
            displays.add(display);
        }

        activeHolograms.put(player.getUniqueId(), displays);
        activeCores.put(player.getUniqueId(), coreLocation);
        activeTemplates.put(player.getUniqueId(), template);

        // 🌟 USO DE DEPENDENCIA INYECTADA
        crossplayUtils.sendMessage(player, "&#55FF55[✓] Plano holográfico proyectado. Comienza a colocar los bloques indicados.");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
    }

    public void clearBlueprint(Player player) {
        var displays = activeHolograms.remove(player.getUniqueId());
        activeCores.remove(player.getUniqueId());
        activeTemplates.remove(player.getUniqueId());
        
        if (displays != null) {
            for (var display : displays) {
                if (display.isValid()) display.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        
        if (!activeTemplates.containsKey(id)) return;

        Location coreLoc = activeCores.get(id);
        StructureTemplate template = activeTemplates.get(id);
        Block placedBlock = event.getBlockPlaced();

        boolean isPart = false;
        
        for (Map.Entry<Vector, Material> entry : template.getRequiredBlocks().entrySet()) {
            Vector rel = entry.getKey();
            Location expectedLoc = coreLoc.clone().add(rel.getBlockX(), rel.getBlockY(), rel.getBlockZ());

            if (placedBlock.getLocation().equals(expectedLoc)) {
                if (placedBlock.getType() == entry.getValue()) {
                    isPart = true;
                    
                    activeHolograms.get(id).removeIf(display -> {
                        if (display.getLocation().getBlockX() == placedBlock.getX() &&
                                display.getLocation().getBlockY() == placedBlock.getY() &&
                                display.getLocation().getBlockZ() == placedBlock.getZ()) {
                            display.remove();
                            return true;
                        }
                        return false;
                    });
                    
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f);
                } else {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Pieza estructural incorrecta. Se requiere: &#FFAA00" + entry.getValue().name());
                    event.setCancelled(true);
                    return;
                }
                break;
            }
        }

        if (isPart && template.isValid(coreLoc.getBlock())) {
            
            // 🛡️ Verificamos la dependencia de forma segura
            if (claimManager == null) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Subsistema de protecciones offline. No se puede enlazar la maquinaria.");
                event.setCancelled(true);
                return;
            }

            var stone = claimManager.getStoneAt(coreLoc);
            if (stone == null) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Las fábricas solo pueden operar dentro de un campo de protección (Nexo-Piedra).");
                event.setCancelled(true);
                return;
            }

            var factory = new ActiveFactory(
                    UUID.randomUUID(), stone.getStoneId(), player.getUniqueId(),
                    template.getFactoryType(), 1, "OFFLINE", 0, coreLoc,
                    "NONE", "NONE", System.currentTimeMillis()
            );

            // 🌟 USO DEL MANAGER INYECTADO (Asíncrono)
            factoryManager.createFactoryAsync(factory).thenRun(() -> {
                crossplayUtils.sendMessage(player, " ");
                crossplayUtils.sendMessage(player, "&#00f5ff✨ <bold>ENSAMBLAJE COMPLETADO</bold>");
                crossplayUtils.sendMessage(player, "&#E6CCFFEstructura registrada como: &#ff00ff" + template.getFactoryType());
                crossplayUtils.sendMessage(player, "&#E6CCFFLa maquinaria se ha enlazado a tu Nexo-Piedra.");
                crossplayUtils.sendMessage(player, " ");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            });

            clearBlueprint(player);
        }
    }
}
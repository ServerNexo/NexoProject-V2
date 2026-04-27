package me.nexo.factories.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.core.ActiveFactory;
import me.nexo.factories.core.StructureTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏭 NexoFactories - Proyector de Hologramas de Construcción (Arquitectura Skyblock AAA)
 * Rendimiento: Paper Native Entity Spawning, Lectura de JSON en PDC y Cero Acoplamiento.
 */
@Singleton
public class BlueprintManager implements Listener {

    private final FactoryManager factoryManager;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, List<BlockDisplay>> activeHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Location> activeCores = new ConcurrentHashMap<>();
    private final Map<UUID, StructureTemplate> activeTemplates = new ConcurrentHashMap<>();

    @Inject
    public BlueprintManager(FactoryManager factoryManager, CrossplayUtils crossplayUtils) {
        this.factoryManager = factoryManager;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 📖 LECTOR DE PLANOS (NUEVO)
    // ==========================================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlueprintInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        // Buscamos si el ítem tiene el ADN inyectado (El JSON comprimido)
        NamespacedKey dataKey = new NamespacedKey("nexofactories", "blueprint_data");
        if (!item.getItemMeta().getPersistentDataContainer().has(dataKey, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String json = item.getItemMeta().getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);

        try {
            // Reconstruimos la estructura matemática desde el JSON
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            String factoryType = jsonObject.get("factory_type").getAsString();
            JsonObject blocks = jsonObject.getAsJsonObject("blocks");

            StructureTemplate template = new StructureTemplate(factoryType);

            for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
                String[] coords = entry.getKey().split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);
                Material mat = Material.valueOf(entry.getValue().getAsString());
                template.addBlock(x, y, z, mat);
            }

            // 🚀 Lanzamos el proyector usando el bloque clicado como núcleo
            projectBlueprint(player, clicked.getLocation(), template);

        } catch (Exception e) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] El plano está corrupto o pertenece a una versión antigua.");
        }
    }

    // ==========================================
    // 📐 PROYECTOR HOLOGRÁFICO
    // ==========================================
    public void projectBlueprint(Player player, Location coreLocation, StructureTemplate template) {
        // 🛡️ FIX ANTI-ENCIMADO: Si ya hay una máquina ahí, cancelamos la proyección.
        if (factoryManager.getFactoryAt(coreLocation) != null) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] El espacio está ocupado. Ya existe un núcleo industrial aquí.");
            return;
        }

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

    // ==========================================
    // 🔨 ENSAMBLAJE FÍSICO
    // ==========================================
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

            var factory = new ActiveFactory(
                    UUID.randomUUID(), new UUID(0, 0), player.getUniqueId(),
                    template.getFactoryType(), 1, "ACTIVE", 0, coreLoc,
                    "NONE", "NONE", System.currentTimeMillis()
            );

            factoryManager.createFactoryAsync(factory).thenRun(() -> {
                crossplayUtils.sendMessage(player, " ");
                crossplayUtils.sendMessage(player, "&#00f5ff✨ <bold>ENSAMBLAJE COMPLETADO</bold>");
                crossplayUtils.sendMessage(player, "&#E6CCFFEstructura registrada como: &#ff00ff" + template.getFactoryType());
                crossplayUtils.sendMessage(player, "&#E6CCFFLa maquinaria industrial ha sido encendida.");
                crossplayUtils.sendMessage(player, " ");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            });

            clearBlueprint(player);
        }
    }
}
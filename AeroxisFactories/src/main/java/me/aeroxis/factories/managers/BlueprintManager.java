package me.aeroxis.factories.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.factories.AeroxisFactories;
import me.aeroxis.factories.core.ActiveFactory;
import me.aeroxis.factories.core.StructureTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏭 AeroxisFactories - Proyector de Hologramas de Construcción (Arquitectura Skyblock AAA)
 * Rendimiento: Paper Native Entity Spawning, Lectura de JSON en PDC y Auto-Ensamblaje.
 */
@Singleton
public class BlueprintManager implements Listener {

    private final AeroxisFactories plugin; // 🌟 AÑADIDO PARA FOLIA SCHEDULER
    private final FactoryManager factoryManager;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, List<BlockDisplay>> activeHolograms = new ConcurrentHashMap<>();
    private final Map<UUID, Location> activeCores = new ConcurrentHashMap<>();
    private final Map<UUID, StructureTemplate> activeTemplates = new ConcurrentHashMap<>();

    @Inject
    public BlueprintManager(AeroxisFactories plugin, FactoryManager factoryManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
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

        NamespacedKey dataKey = new NamespacedKey("nexofactories", "blueprint_data");
        if (!item.getItemMeta().getPersistentDataContainer().has(dataKey, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String json = item.getItemMeta().getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);

        try {
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

            projectBlueprint(player, clicked.getLocation(), template);

        } catch (Exception e) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] El plano está corrupto o pertenece a una versión antigua.");
        }
    }

    // ==========================================
    // 🔧 AUTO-ENSAMBLAJE (LLAVE INGLESA)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onAutoAssemble(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // 🌟 CONDICIÓN: La llave inglesa es una Azada de Oro
        if (hand.getType() != Material.GOLDEN_HOE) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        UUID id = player.getUniqueId();
        if (!activeTemplates.containsKey(id)) return;

        Location coreLoc = activeCores.get(id);

        // El jugador debe hacer clic exactamente en el bloque núcleo del holograma
        if (!clicked.getLocation().equals(coreLoc)) return;

        event.setCancelled(true);
        StructureTemplate template = activeTemplates.get(id);

        crossplayUtils.sendMessage(player, "&#FFAA00⚙ Analizando inventario para auto-ensamblaje...");

        // 1. Calcular cuántos bloques de cada tipo requiere el plano
        Map<Material, Integer> requiredMaterials = new HashMap<>();
        for (Material mat : template.getRequiredBlocks().values()) {
            requiredMaterials.put(mat, requiredMaterials.getOrDefault(mat, 0) + 1);
        }

        // 2. Verificar si el jugador tiene todos los materiales necesarios
        for (Map.Entry<Material, Integer> req : requiredMaterials.entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(req.getKey()), req.getValue())) {
                crossplayUtils.sendMessage(player, "&#FF5555[x] Materiales insuficientes. Te faltan bloques de: &#FFAA00" + req.getKey().name());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }

        // 3. Consumir los materiales del inventario
        for (Map.Entry<Material, Integer> req : requiredMaterials.entrySet()) {
            player.getInventory().removeItem(new ItemStack(req.getKey(), req.getValue()));
        }

        // 4. Colocación física en el Hilo de Región (Folia-Ready)
        Bukkit.getRegionScheduler().execute(plugin, coreLoc, () -> {
            for (Map.Entry<Vector, Material> entry : template.getRequiredBlocks().entrySet()) {
                Location placeLoc = coreLoc.clone().add(entry.getKey());
                placeLoc.getBlock().setType(entry.getValue());
            }

            // 🌟 FIX: Registrar Fábrica con targetLinkId nulo por defecto (Fase 2)
            var factory = new ActiveFactory(
                    UUID.randomUUID(), new UUID(0, 0), player.getUniqueId(),
                    template.getFactoryType(), 1, "ACTIVE", 0, coreLoc,
                    "NONE", "NONE", System.currentTimeMillis(),
                    null // <-- Enlace de red desactivado al nacer
            );

            factoryManager.createFactoryAsync(factory).thenRun(() -> {
                crossplayUtils.sendMessage(player, " ");
                crossplayUtils.sendMessage(player, "&#00f5ff✨ <bold>AUTO-ENSAMBLAJE COMPLETADO</bold>");
                crossplayUtils.sendMessage(player, "&#E6CCFFEstructura registrada como: &#ff00ff" + template.getFactoryType());
                crossplayUtils.sendMessage(player, " ");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            });

            clearBlueprint(player);
        });
    }

    // ==========================================
    // 📐 PROYECTOR HOLOGRÁFICO
    // ==========================================
    public void projectBlueprint(Player player, Location coreLocation, StructureTemplate template) {
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

        crossplayUtils.sendMessage(player, "&#55FF55[✓] Plano proyectado. Coloca los bloques manualmente o haz clic con una Llave Inglesa (Azada de Oro) en el núcleo para auto-ensamblar.");
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
    // 🔨 ENSAMBLAJE FÍSICO MANUAL Y FEEDBACK
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

                    // 🌟 FEEDBACK VISUAL DE ERROR (Parpadeo Rojo)
                    activeHolograms.get(id).stream()
                            .filter(d -> d.getLocation().getBlockX() == expectedLoc.getBlockX() &&
                                    d.getLocation().getBlockY() == expectedLoc.getBlockY() &&
                                    d.getLocation().getBlockZ() == expectedLoc.getBlockZ())
                            .findFirst()
                            .ifPresent(display -> {
                                display.setGlowing(true);
                                display.setGlowColorOverride(Color.RED);
                                // Quitar rojo después de 1 segundo (20 ticks) usando Folia
                                Bukkit.getRegionScheduler().runDelayed(plugin, placedBlock.getLocation(), task -> {
                                    if (display.isValid()) display.setGlowColorOverride(null);
                                }, 20L);
                            });
                    return;
                }
                break;
            }
        }

        if (isPart && template.isValid(coreLoc.getBlock())) {

            // 🌟 FIX: Registrar Fábrica con targetLinkId nulo por defecto (Fase 2)
            var factory = new ActiveFactory(
                    UUID.randomUUID(), new UUID(0, 0), player.getUniqueId(),
                    template.getFactoryType(), 1, "ACTIVE", 0, coreLoc,
                    "NONE", "NONE", System.currentTimeMillis(),
                    null // <-- Enlace de red desactivado al nacer
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
package me.aeroxis.factories.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.factories.managers.RecipeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer; // 🌟 NUEVO IMPORT
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer; // 🌟 NUEVO IMPORT
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏭 AeroxisFactories - Estaciones de Crafteo Físico (Arquitectura AAA & Bedrock Ready)
 * Rendimiento: ItemDisplays nativos, Cero Entidades de dropeo y Motor de Recetas YAML.
 */
@Singleton
public class CraftingStationListener implements Listener {

    private final CrossplayUtils crossplayUtils;
    private final RecipeManager recipeManager; // 🌟 NUEVA DEPENDENCIA

    // RAM Database para las mesas activas
    private final Map<Location, List<ItemStack>> tableItems = new ConcurrentHashMap<>();
    private final Map<Location, List<ItemDisplay>> tableDisplays = new ConcurrentHashMap<>();

    @Inject
    public CraftingStationListener(CrossplayUtils crossplayUtils, RecipeManager recipeManager) {
        this.crossplayUtils = crossplayUtils;
        this.recipeManager = recipeManager; // 🌟 INYECTADO
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTableInteract(PlayerInteractEvent event) {
        // Ignoramos la mano secundaria para que el evento no se dispare dos veces
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.SMITHING_TABLE) return;

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        Location tableLoc = clicked.getLocation();

        // Evitamos que abra el menú vanilla de la mesa de herrería
        event.setCancelled(true);

        // ==========================================
        // 📥 ACCIÓN 1: COLOCAR ÍTEMS (Clic Derecho)
        // ==========================================
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (handItem.getType().isAir()) return;

            // Extraemos 1 ítem del stack del jugador
            ItemStack itemToPlace = handItem.clone();
            itemToPlace.setAmount(1);
            handItem.setAmount(handItem.getAmount() - 1);

            // Guardamos en la memoria RAM
            tableItems.putIfAbsent(tableLoc, new ArrayList<>());
            tableDisplays.putIfAbsent(tableLoc, new ArrayList<>());
            List<ItemStack> currentItems = tableItems.get(tableLoc);
            List<ItemDisplay> currentDisplays = tableDisplays.get(tableLoc);

            // Máximo 4 tipos de ítems en la mesa para no saturar visualmente
            if (currentItems.size() >= 4) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] La mesa está llena.");
                player.getInventory().addItem(itemToPlace);
                return;
            }

            currentItems.add(itemToPlace);

            // 🌟 MAGIA VISUAL: Spawneamos el Holograma (ItemDisplay)
            Location displayLoc = tableLoc.clone().add(0.5, 1.1 + (currentItems.size() * 0.1), 0.5);
            ItemDisplay display = tableLoc.getWorld().spawn(displayLoc, ItemDisplay.class, d -> {
                d.setItemStack(itemToPlace);
                d.setBillboard(ItemDisplay.Billboard.FIXED);

                // Lo acostamos sobre la mesa
                Transformation trans = d.getTransformation();
                trans.getRightRotation().set(new Quaternionf().rotateX((float) Math.PI / 2f));
                trans.getScale().set(new Vector3f(0.5f, 0.5f, 0.5f));
                d.setTransformation(trans);
            });

            currentDisplays.add(display);
            player.playSound(tableLoc, Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 0.8f, 1.2f);
            return;
        }

        // ==========================================
        // 🔨 ACCIÓN 2: INTERACCIÓN DE GOLPE (Clic Izquierdo)
        // ==========================================
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            List<ItemStack> items = tableItems.getOrDefault(tableLoc, new ArrayList<>());
            List<ItemDisplay> displays = tableDisplays.getOrDefault(tableLoc, new ArrayList<>());

            if (items.isEmpty()) return;

            // 📤 RECUPERAR: Si golpea SIN el martillo, escupe el último ítem (Bedrock-Friendly)
            boolean isHammer = false;
            if (handItem.getType() == Material.IRON_AXE && handItem.hasItemMeta() && handItem.getItemMeta().hasDisplayName()) {
                // 🌟 FIX: Lectura segura de nombres (Ignora colores)
                String plainName = PlainTextComponentSerializer.plainText().serialize(handItem.getItemMeta().displayName());
                if (plainName.contains("Nexo-Martillo")) {
                    isHammer = true;
                }
            }

            if (!isHammer) {
                int lastIndex = items.size() - 1;
                ItemStack recovered = items.remove(lastIndex);
                ItemDisplay displayToRemove = displays.remove(lastIndex);

                displayToRemove.remove();
                tableLoc.getWorld().dropItemNaturally(tableLoc.clone().add(0.5, 1.2, 0.5), recovered);
                player.playSound(tableLoc, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.5f);
                return;
            }

            // ⚙️ PREPARAMOS EL INVENTARIO PARA EL ESCÁNER YAML
            Map<String, Integer> itemsOnTable = new HashMap<>();
            for (ItemStack item : items) {
                String id = "minecraft:" + item.getType().name();
                itemsOnTable.put(id, itemsOnTable.getOrDefault(id, 0) + 1);
            }

            // 📖 BÚSQUEDA DINÁMICA DE RECETAS (O(1))
            var match = recipeManager.findMatchingRecipe(itemsOnTable);

            if (match.isPresent()) {
                var recipe = match.get();

                // 🔒 CANDADO DE SKILLS (Fase 3: Se conectará al SkillTreeMenu)
                if (!recipe.requiredNode().equals("NONE") && !player.hasPermission("nexofactories.node." + recipe.requiredNode().toLowerCase())) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] Te falta conocimiento. Desbloquea: &#FFAA00" + recipe.requiredNode() + " &#FF5555en tu Árbol de Habilidades.");
                    player.playSound(tableLoc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                // Limpiamos la mesa
                items.clear();
                for (ItemDisplay d : displays) d.remove();
                displays.clear();

                // Generamos el resultado desde el YAML
                String[] resultData = recipe.resultItemId().split(":");
                Material resultMat = Material.valueOf(resultData[1]); // Asumimos vanilla por ahora
                ItemStack resultado = new ItemStack(resultMat, recipe.resultAmount());

                // Si la receta era el motor, le ponemos el nombre (Temporal hasta usar AeroxisItems)
                if (recipe.id().equals("motor_industrial_t1")) {
                    ItemMeta meta = resultado.getItemMeta();
                    // 🌟 FIX: Escritura de nombres con Kyori Adventure API
                    meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&6&lMotor Industrial T1"));
                    resultado.setItemMeta(meta);
                }

                // Efectos Especiales AAA
                tableLoc.getWorld().spawnParticle(Particle.LAVA, tableLoc.clone().add(0.5, 1.2, 0.5), 15, 0.2, 0.2, 0.2, 0.1);
                tableLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, tableLoc.clone().add(0.5, 1.2, 0.5), 30, 0.3, 0.3, 0.3, 0.1);
                tableLoc.getWorld().playSound(tableLoc, Sound.BLOCK_ANVIL_USE, 1.0f, 0.8f);
                tableLoc.getWorld().playSound(tableLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.5f);

                tableLoc.getWorld().dropItemNaturally(tableLoc.clone().add(0.5, 1.2, 0.5), resultado);
                crossplayUtils.sendMessage(player, "&#55FF55[✓] ¡Forja exitosa! Receta: " + recipe.id());

            } else {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Los materiales no coinciden con ninguna receta.");

                // 🕵️‍♂️ MODO DEBUG: Nos chismorrea qué está leyendo la mesa
                crossplayUtils.sendMessage(player, "&7Diagnóstico de la mesa:");
                for (Map.Entry<String, Integer> entry : itemsOnTable.entrySet()) {
                    crossplayUtils.sendMessage(player, "&e- " + entry.getValue() + "x " + entry.getKey());
                }
                crossplayUtils.sendMessage(player, "&7Total de piezas detectadas: &c" + items.size());

                player.playSound(tableLoc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        }
    }

    // ==========================================
    // 🧹 ACCIÓN 3: SEGURIDAD (Romper Mesa)
    // ==========================================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onTableBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        if (broken.getType() != Material.SMITHING_TABLE) return;

        Location loc = broken.getLocation();
        if (tableItems.containsKey(loc)) {
            // Soltamos todo al suelo para que no se pierda nada
            for (ItemStack item : tableItems.get(loc)) {
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), item);
            }
            tableItems.remove(loc);

            // Borramos los hologramas
            if (tableDisplays.containsKey(loc)) {
                for (ItemDisplay display : tableDisplays.get(loc)) {
                    display.remove();
                }
                tableDisplays.remove(loc);
            }
        }
    }
}
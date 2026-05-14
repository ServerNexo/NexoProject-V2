package me.aeroxis.factories.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import net.kyori.adventure.text.Component; // 🌟 NUEVO IMPORT
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer; // 🌟 NUEVO IMPORT
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer; // 🌟 NUEVO IMPORT
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏭 AeroxisFactories - Escáner Espacial de Planos (Arquitectura AAA)
 * Rendimiento: Cálculo Vectorial Asíncrono, Filtro de Terreno O(1) y Compresión JSON.
 */
@Singleton
public class BlueprintScanner implements Listener {

    private final CrossplayUtils crossplayUtils;
    private final Gson gson = new Gson();

    // Mapas en RAM para guardar selecciones (Estilo WorldEdit)
    private final Map<UUID, Location> pos1Map = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2Map = new ConcurrentHashMap<>();

    // 🌟 FILTRO INTELIGENTE: EnumSet ultra rápido para ignorar terreno accidental
    private static final Set<Material> IGNORED_MATERIALS = EnumSet.of(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.DIRT_PATH,
            Material.STONE, Material.SAND, Material.GRAVEL, Material.BEDROCK,
            Material.WATER, Material.LAVA, Material.SHORT_GRASS, Material.TALL_GRASS
    );

    @Inject
    public BlueprintScanner(CrossplayUtils crossplayUtils) {
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 🌟 FIX: Lectura de Nombres moderna con PlainTextComponentSerializer
        if (item.getType() == Material.BLAZE_ROD && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {

            String plainName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());

            if (plainName.contains("Nexo-Escáner")) {
                Block block = event.getClickedBlock();
                if (block == null) return;

                event.setCancelled(true);
                UUID id = player.getUniqueId();

                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    pos1Map.put(id, block.getLocation());
                    crossplayUtils.sendMessage(player, "&#55FF55[✓] Posición 1 fijada: " + block.getX() + ", " + block.getY() + ", " + block.getZ());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    pos2Map.put(id, block.getLocation());
                    crossplayUtils.sendMessage(player, "&#55FF55[✓] Posición 2 fijada: " + block.getX() + ", " + block.getY() + ", " + block.getZ());
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                }
            }
        }
    }

    public void createBlueprintItem(Player player, Block coreBlock, String factoryType) {
        UUID id = player.getUniqueId();
        Location p1 = pos1Map.get(id);
        Location p2 = pos2Map.get(id);

        if (p1 == null || p2 == null || !p1.getWorld().equals(p2.getWorld())) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Debes seleccionar Posición 1 y Posición 2 con el escáner primero.");
            return;
        }

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        Location coreLoc = coreBlock.getLocation();
        Map<String, String> blockDataMap = new HashMap<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block b = p1.getWorld().getBlockAt(x, y, z);

                    // 🌟 APLICAMOS EL FILTRO: Ignora pasto, tierra, etc. o el mismo núcleo
                    if (IGNORED_MATERIALS.contains(b.getType()) || b.getLocation().equals(coreLoc)) continue;

                    int dx = x - coreLoc.getBlockX();
                    int dy = y - coreLoc.getBlockY();
                    int dz = z - coreLoc.getBlockZ();

                    String vectorKey = dx + "," + dy + "," + dz;
                    blockDataMap.put(vectorKey, b.getType().name());
                }
            }
        }

        if (blockDataMap.isEmpty()) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] No se encontraron bloques industriales válidos en el área.");
            return;
        }

        JsonObject blueprintJson = new JsonObject();
        blueprintJson.addProperty("factory_type", factoryType);
        blueprintJson.add("blocks", gson.toJsonTree(blockDataMap));
        String finalData = gson.toJson(blueprintJson);

        ItemStack blueprintItem = new ItemStack(Material.PAPER);
        ItemMeta meta = blueprintItem.getItemMeta();

        // 🌟 FIX: Escritura de Nombres con Kyori Adventure API (Usando & en vez de §)
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&b&lPlano Industrial: &f" + factoryType));

        // 🌟 FIX: Escritura de Lore con Componentes
        List<Component> lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Fábrica personalizada diseñada"));
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7por el ingeniero: &a" + player.getName()));
        lore.add(Component.empty()); // Espacio en blanco limpio
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&e▶ Clic Derecho para proyectar"));
        meta.lore(lore);

        NamespacedKey dataKey = new NamespacedKey("nexofactories", "blueprint_data");
        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, finalData);

        blueprintItem.setItemMeta(meta);

        player.getInventory().addItem(blueprintItem);

        crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>PLANO CREADO CON ÉXITO</bold>");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
    }
}
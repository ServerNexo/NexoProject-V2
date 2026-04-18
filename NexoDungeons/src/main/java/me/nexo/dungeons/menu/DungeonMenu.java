package me.nexo.dungeons.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.dungeons.NexoDungeons;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🏰 NexoDungeons - Menú Principal de Mazmorras (Arquitectura Enterprise)
 * Nota: Como es instanciado por jugador, no requiere @Singleton ni @Inject.
 */
public class DungeonMenu extends NexoMenu {

    private final NexoDungeons plugin;

    public DungeonMenu(Player player, NexoDungeons plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        // 🌟 FIX: Título directo y serializado seguro para Bedrock. ¡Cero lag I/O!
        return LegacyComponentSerializer.legacySection().serialize(
                NexoColor.parse("&#8b0000🏰 <bold>MAZMORRAS DEL NEXO</bold>")
        );
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // El cristal morado automático de tu NexoMenu

        // 🚪 1. MAZMORRAS INSTANCIADAS
        ItemStack instanced = new ItemStack(Material.IRON_DOOR);
        ItemMeta instancedMeta = instanced.getItemMeta();
        if (instancedMeta != null) {
            instancedMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#FFAA00🚪 <bold>FORTALEZAS INSTANCIADAS</bold>"));

            // 🌟 FIX: Lore directo. Cero lag de lectura I/O.
            List<String> loreRaw = List.of(
                    "&#E6CCFFExplora calabozos generados",
                    "&#E6CCFFproceduralmente en el Vacío.",
                    "&#E6CCFFResuelve acertijos y sobrevive.",
                    "",
                    "&#00f5ff► Clic para buscar fortaleza"
            );
            instancedMeta.lore(loreRaw.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));

            // MAGIA PDC: Guardamos la acción
            instancedMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "open_instanced");
            instanced.setItemMeta(instancedMeta);
        }
        inventory.setItem(11, instanced);

        // ⚔️ 2. ARENAS DE SUPERVIVENCIA (Olas)
        ItemStack waves = new ItemStack(Material.IRON_SWORD);
        ItemMeta wavesMeta = waves.getItemMeta();
        if (wavesMeta != null) {
            wavesMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#FF5555⚔ <bold>ARENAS DE SUPERVIVENCIA</bold>"));

            List<String> loreRaw = List.of(
                    "&#E6CCFFÚnete a la cola de emparejamiento",
                    "&#E6CCFFy enfréntate a oleadas de enemigos",
                    "&#E6CCFFjunto a tu escuadrón.",
                    "",
                    "&#00f5ff► Clic para unirse a la cola"
            );
            wavesMeta.lore(loreRaw.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));

            wavesMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "join_waves");
            waves.setItemMeta(wavesMeta);
        }
        inventory.setItem(13, waves);

        // 🐉 3. JEFES GLOBALES
        ItemStack worldBoss = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta bossMeta = worldBoss.getItemMeta();
        if (bossMeta != null) {
            bossMeta.displayName(CrossplayUtils.parseCrossplay(player, "&#ff00ff🐉 <bold>AMENAZAS GLOBALES</bold>"));

            List<String> loreRaw = List.of(
                    "&#E6CCFFEventos masivos donde todo",
                    "&#E6CCFFel servidor debe unirse para",
                    "&#E6CCFFderrotar a un titán.",
                    "",
                    "&#00f5ff► Clic para ver información"
            );
            bossMeta.lore(loreRaw.stream().map(line -> CrossplayUtils.parseCrossplay(player, line)).collect(Collectors.toList()));

            bossMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, "world_boss");
            worldBoss.setItemMeta(bossMeta);
        }
        inventory.setItem(15, worldBoss);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto anti-robos

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey actionKey = new NamespacedKey(plugin, "action");

        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if (action.equals("open_instanced")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                CrossplayUtils.sendMessage(player, "&#FFAA00[⏳] Sincronizando coordenadas en el Vacío... Buscando fortalezas.");
                player.closeInventory();

            } else if (action.equals("join_waves")) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
                player.closeInventory();
                // Delegamos al QueueManager que ya inyectamos en Main
                plugin.getQueueManager().addPlayerToWaves(player);

            } else if (action.equals("world_boss")) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Los altares de invocación se encuentran en las profundidades del mundo abierto.");
                player.closeInventory();
            }
        }
    }
}
package me.nexo.dungeons.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.dungeons.matchmaking.QueueManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 🏰 NexoDungeons - Menú Principal de Mazmorras (Arquitectura Enterprise Java 21)
 * Rendimiento: editMeta O(1), Cero Instanciación de Llaves (GC Safe) e Inyección Transitiva.
 */
public class DungeonMenu extends NexoMenu {

    // 🌟 DEPENDENCIAS PROPAGADAS (Entregadas por el ComandoDungeon)
    private final QueueManager queueManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 OPTIMIZACIÓN GC: Llave instanciada una única vez
    private final NamespacedKey actionKey;

    public DungeonMenu(Player player, QueueManager queueManager, CrossplayUtils crossplayUtils) {
        super(player, crossplayUtils); // 🌟 FIX ERROR SUPER: Pasamos la herramienta a la clase base

        this.queueManager = queueManager;
        this.crossplayUtils = crossplayUtils;

        // En Paper 1.21.5 instanciamos llaves de forma estática para ahorrar memoria
        this.actionKey = new NamespacedKey("nexodungeons", "action");
    }

    @Override
    public String getMenuName() {
        return "&#8b0000🏰 <bold>MAZMORRAS DEL NEXO</bold>";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🚪 1. MAZMORRAS INSTANCIADAS
        var instanced = new ItemStack(Material.IRON_DOOR);
        instanced.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00🚪 <bold>FORTALEZAS INSTANCIADAS</bold>"));

            var loreRaw = List.of(
                    "&#E6CCFFExplora calabozos generados",
                    "&#E6CCFFproceduralmente en el Vacío.",
                    "&#E6CCFFResuelve acertijos y sobrevive.",
                    "",
                    "&#00f5ff► Clic para buscar fortaleza"
            );

            // 🌟 JAVA 21 NATIVO: .toList() es más rápido e inmutable
            meta.lore(loreRaw.stream().map(line -> crossplayUtils.parseCrossplay(player, line)).toList());

            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_instanced");
        });
        inventory.setItem(11, instanced);

        // ⚔️ 2. ARENAS DE SUPERVIVENCIA (Olas)
        var waves = new ItemStack(Material.IRON_SWORD);
        waves.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555⚔ <bold>ARENAS DE SUPERVIVENCIA</bold>"));

            var loreRaw = List.of(
                    "&#E6CCFFÚnete a la cola de emparejamiento",
                    "&#E6CCFFy enfréntate a oleadas de enemigos",
                    "&#E6CCFFjunto a tu escuadrón.",
                    "",
                    "&#00f5ff► Clic para unirse a la cola"
            );
            meta.lore(loreRaw.stream().map(line -> crossplayUtils.parseCrossplay(player, line)).toList());

            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "join_waves");
        });
        inventory.setItem(13, waves);

        // 🐉 3. JEFES GLOBALES
        var worldBoss = new ItemStack(Material.DRAGON_HEAD);
        worldBoss.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff🐉 <bold>AMENAZAS GLOBALES</bold>"));

            var loreRaw = List.of(
                    "&#E6CCFFEventos masivos donde todo",
                    "&#E6CCFFel servidor debe unirse para",
                    "&#E6CCFFderrotar a un titán.",
                    "",
                    "&#00f5ff► Clic para ver información"
            );
            meta.lore(loreRaw.stream().map(line -> crossplayUtils.parseCrossplay(player, line)).toList());

            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "world_boss");
        });
        inventory.setItem(15, worldBoss);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // Bloqueo absoluto anti-robos

        var item = event.getCurrentItem();

        // 🌟 PAPER 1.21 FIX: isEmpty() es la forma segura de descartar ítems nulos/aire
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();

        if (meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

            if ("open_instanced".equals(action)) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                crossplayUtils.sendMessage(player, "&#FFAA00[⏳] Sincronizando coordenadas en el Vacío... Buscando fortalezas.");
                player.closeInventory();

            } else if ("join_waves".equals(action)) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
                player.closeInventory();

                // 🌟 INYECCIÓN: Llamada limpia al Manager inyectado
                queueManager.addPlayerToWaves(player);

            } else if ("world_boss".equals(action)) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                crossplayUtils.sendMessage(player, "&#FF5555[!] Los altares de invocación se encuentran en las profundidades del mundo abierto.");
                player.closeInventory();
            }
        }
    }
}
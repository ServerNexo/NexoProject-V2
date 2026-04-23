package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.colecciones.data.Tier;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 📚 NexoColecciones - Menú Interactivo (Arquitectura Enterprise)
 * Rendimiento: Cero Lag Visual (0 I/O), Llaves Cacheadas y Dependencias Propagadas.
 * Nota: Al ser una GUI transitoria (1 por jugador), NO lleva @Singleton.
 */
public class ColeccionesMenu extends NexoMenu {

    private final NexoColecciones plugin;
    private final CollectionManager collectionManager; // 🌟 Sinergia inyectada
    private final CrossplayUtils crossplayUtils;       // 🌟 Sinergia inyectada

    private final MenuType menuType;
    private final String categoryId;
    private final String itemId;

    // 🌟 OPTIMIZACIÓN DE RAM: Llaves de PDC cacheadas para no instanciarlas en bucles
    private final NamespacedKey actionKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey itemKey;
    private final NamespacedKey tierKey;

    public enum MenuType { MAIN, CATEGORY, ITEM_TIERS }

    public ColeccionesMenu(Player player, NexoColecciones plugin, CollectionManager collectionManager, 
                           CrossplayUtils crossplayUtils, MenuType type, String categoryId, String itemId) {
        super(player);
        this.plugin = plugin;
        this.collectionManager = collectionManager;
        this.crossplayUtils = crossplayUtils;
        
        this.menuType = type;
        this.categoryId = categoryId;
        this.itemId = itemId;

        this.actionKey = new NamespacedKey(plugin, "action");
        this.categoryKey = new NamespacedKey(plugin, "category_id");
        this.itemKey = new NamespacedKey(plugin, "item_id");
        this.tierKey = new NamespacedKey(plugin, "tier_level");
    }

    @Override
    public String getMenuName() {
        if (menuType == MenuType.MAIN) return "&#FFAA00📚 <bold>TUS COLECCIONES</bold>";
        if (menuType == MenuType.CATEGORY) {
            var cat = collectionManager.getCategorias().get(categoryId);
            return cat != null ? cat.getNombre() : "&#FFAA00📚 Categoría";
        }
        return "&#E6CCFF⭐ <bold>PROGRESO DEL ÍTEM</bold>";
    }

    @Override
    public int getSlots() {
        return menuType == MenuType.MAIN ? 27 : (menuType == MenuType.CATEGORY ? 54 : 45);
    }

    @Override
    public void setMenuItems() {
        setFillerGlass(); // Añade el cristal morado automáticamente (desde NexoMenu)

        // ==========================================
        // 📚 1. MENÚ PRINCIPAL (CATEGORÍAS)
        // ==========================================
        if (menuType == MenuType.MAIN) {
            for (CollectionCategory cat : collectionManager.getCategorias().values()) {
                var item = new ItemStack(cat.getIcono());
                var meta = item.getItemMeta();
                
                if (meta != null) {
                    meta.displayName(crossplayUtils.parseCrossplay(player, cat.getNombre()));

                    // 🌟 FIX: Textos directos y parseados con la instancia
                    List<net.kyori.adventure.text.Component> lore = List.of(
                            crossplayUtils.parseCrossplay(player, "&#555555Categoría de Farmeo"),
                            crossplayUtils.parseCrossplay(player, ""),
                            crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para explorar")
                    );
                    meta.lore(lore);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_category");
                    meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, cat.getId());
                    item.setItemMeta(meta);
                }
                inventory.setItem(cat.getSlot(), item);
            }
        }
        // ==========================================
        // 📦 2. MENÚ DE CATEGORÍA (ÍTEMS)
        // ==========================================
        else if (menuType == MenuType.CATEGORY) {
            var cat = collectionManager.getCategorias().get(categoryId);
            if (cat == null) return;

            var profile = collectionManager.getProfile(player.getUniqueId());

            for (CollectionItem cItem : cat.getItems().values()) {
                int progreso = profile != null ? profile.getProgress(cItem.getId()) : 0;
                int nivelActual = collectionManager.calcularNivel(cItem, progreso);

                ItemStack item;
                
                if (progreso == 0) {
                    item = new ItemStack(Material.GRAY_DYE); // Color gris para no descubiertos
                    var meta = item.getItemMeta();
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555??? (Desconocido)"));
                    meta.lore(List.of(
                            crossplayUtils.parseCrossplay(player, "&#555555Sigue explorando y farmeando"),
                            crossplayUtils.parseCrossplay(player, "&#555555para descubrir esta colección.")
                    ));
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    item.setItemMeta(meta);
                } else {
                    item = new ItemStack(cItem.getIcono());
                    var meta = item.getItemMeta();
                    meta.displayName(crossplayUtils.parseCrossplay(player, cItem.getNombre()));
                    meta.lore(List.of(
                            crossplayUtils.parseCrossplay(player, "&#E6CCFFNivel de Maestría: &#FFAA00" + nivelActual + " / " + cItem.getMaxTier()),
                            crossplayUtils.parseCrossplay(player, "&#E6CCFFProgreso Total: &#55FF55" + progreso),
                            crossplayUtils.parseCrossplay(player, ""),
                            crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para ver recompensas")
                    ));

                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_item");
                    meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, cItem.getId());
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    item.setItemMeta(meta);
                }
                inventory.setItem(cItem.getSlotMenu(), item);
            }
            addBackButton("main");
        }
        // ==========================================
        // ⭐ 3. MENÚ DE TIERS (RECOMPENSAS)
        // ==========================================
        else if (menuType == MenuType.ITEM_TIERS) {
            var cItem = collectionManager.getItemGlobal(itemId);
            if (cItem == null) return;

            var profile = collectionManager.getProfile(player.getUniqueId());
            int progreso = profile != null ? profile.getProgress(cItem.getId()) : 0;

            int[] slotsCentro = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
            int i = 0;
            List<Integer> niveles = new ArrayList<>(cItem.getTiers().keySet());
            Collections.sort(niveles);

            for (int nivel : niveles) {
                if (i >= slotsCentro.length) break;
                var tier = cItem.getTier(nivel);
                boolean desbloqueado = progreso >= tier.getRequerido();
                boolean reclamado = profile != null && profile.hasClaimedTier(cItem.getId(), nivel);

                ItemStack item;
                
                if (reclamado) {
                    item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    var meta = item.getItemMeta();
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55[✓] Nivel " + nivel + " Completado"));
                    meta.lore(List.of(crossplayUtils.parseCrossplay(player, "&#555555Ya has reclamado estas recompensas.")));
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                    item.setItemMeta(meta);
                } else if (desbloqueado) {
                    item = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                    var meta = item.getItemMeta();
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00[!] Nivel " + nivel + " Desbloqueado"));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true); // Brillo

                    List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                    lore.add(crossplayUtils.parseCrossplay(player, "&#E6CCFFMeta alcanzada: &#55FF55" + tier.getRequerido()));
                    lore.add(crossplayUtils.parseCrossplay(player, ""));
                    tier.getLoreRecompensa().forEach(line -> lore.add(crossplayUtils.parseCrossplay(player, line)));
                    lore.add(crossplayUtils.parseCrossplay(player, ""));
                    lore.add(crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para Reclamar"));
                    meta.lore(lore);

                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "claim_tier");
                    meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, nivel);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                    item.setItemMeta(meta);
                } else {
                    item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    var meta = item.getItemMeta();
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555[x] Nivel " + nivel + " Bloqueado"));
                    meta.lore(List.of(crossplayUtils.parseCrossplay(player, "&#E6CCFFFaltan: &#FF5555" + (tier.getRequerido() - progreso) + " &#E6CCFFpara desbloquear.")));
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                    item.setItemMeta(meta);
                }

                inventory.setItem(slotsCentro[i], item);
                i++;
            }

            addBackButton("cat_" + cItem.getCategoriaId());

            // 🏆 Botón de Top 5
            var info = new ItemStack(Material.NETHER_STAR);
            var iMeta = info.getItemMeta();
            iMeta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff🏆 Ránking de Colección"));
            iMeta.lore(List.of(
                    crossplayUtils.parseCrossplay(player, "&#E6CCFFTu farmeo total: &#55FF55" + progreso),
                    crossplayUtils.parseCrossplay(player, ""),
                    crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para ver el TOP 5 Global")
            ));
            iMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "show_top");
            iMeta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, cItem.getId());
            info.setItemMeta(iMeta);
            inventory.setItem(40, info);
        }
    }

    private void addBackButton(String target) {
        var back = new ItemStack(Material.ARROW);
        var meta = back.getItemMeta();
        meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555⬅ Volver Atrás"));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_" + target);
        back.setItemMeta(meta);
        inventory.setItem(getSlots() - 5, back);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true);
        var item = event.getCurrentItem();

        if (item == null || !item.hasItemMeta()) return;

        var meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        // 🌟 FIX: Transiciones rápidas (1 tick) pasando las instancias inyectadas al nuevo menú
        switch (action) {
            case "open_category" -> {
                String catId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, MenuType.CATEGORY, catId, "").open(), 1L);
            }
            case "open_item" -> {
                String iId = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, MenuType.ITEM_TIERS, categoryId, iId).open(), 1L);
            }
            case "claim_tier" -> {
                Integer tierNivel = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
                if (tierNivel != null) {
                    collectionManager.reclamarRecompensa(player, itemId, tierNivel);
                    setMenuItems(); // Actualiza el cristal de amarillo a verde instantáneamente
                }
            }
            case "show_top" -> {
                String iId = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
                collectionManager.calcularTopAsync(player, iId);
            }
            default -> {
                if (action.startsWith("back_")) {
                    String target = action.replace("back_", "");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                    player.closeInventory();

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (target.equals("main")) {
                            new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, MenuType.MAIN, "", "").open();
                        } else if (target.startsWith("cat_")) {
                            new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, MenuType.CATEGORY, target.replace("cat_", ""), "").open();
                        }
                    }, 1L);
                }
            }
        }
    }
}
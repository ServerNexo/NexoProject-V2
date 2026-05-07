package me.nexo.colecciones.menu;

import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.colecciones.data.RewardTemplate; // 🌟 IMPORTACIÓN NUEVA
import me.nexo.colecciones.data.Tier;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment; // 🌟 IMPORTACIÓN NUEVA
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator; // 🌟 IMPORTACIÓN NUEVA
import java.util.List;

/**
 * 📚 NexoColecciones - Menú Interactivo (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Lag Visual (0 I/O), Llaves Cacheadas, editMeta O(1) y Dependencias Propagadas.
 */
public class ColeccionesMenu extends NexoMenu {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoColecciones plugin;
    private final CollectionManager collectionManager;
    private final CrossplayUtils crossplayUtils;

    private final MenuType menuType;
    private final String categoryId;
    private final String itemId;

    // 🌟 OPTIMIZACIÓN DE RAM: Llaves de PDC cacheadas para no instanciarlas en bucles
    private final NamespacedKey actionKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey itemKey;
    private final NamespacedKey tierKey;

    public enum MenuType { MAIN, CATEGORY, ITEM_TIERS }

    // 💉 PILAR 1: Inyección Transitiva
    public ColeccionesMenu(Player player, NexoColecciones plugin, CollectionManager collectionManager,
                           CrossplayUtils crossplayUtils, MenuType type, String categoryId, String itemId) {
        // 🌟 FIX ERROR SUPER: Pasamos la dependencia inyectada a la clase Padre (NexoMenu)
        super(player, crossplayUtils);

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
        // 🌟 FIX MÓDULO 2: El menú de Tiers ahora requiere 54 slots para la forma de S
        return menuType == MenuType.MAIN ? 27 : 54;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // ==========================================
        // 📚 1. MENÚ PRINCIPAL (CATEGORÍAS)
        // ==========================================
        if (menuType == MenuType.MAIN) {
            for (CollectionCategory cat : collectionManager.getCategorias().values()) {
                var item = new ItemStack(cat.getIcono());

                item.editMeta(meta -> {
                    meta.displayName(crossplayUtils.parseCrossplay(player, cat.getNombre()));

                    List<net.kyori.adventure.text.Component> lore = List.of(
                            crossplayUtils.parseCrossplay(player, "&#555555Categoría de Farmeo"),
                            net.kyori.adventure.text.Component.empty(),
                            crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para explorar")
                    );
                    meta.lore(lore);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_category");
                    meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, cat.getId());
                });

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
                    item.editMeta(meta -> {
                        meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555??? (Desconocido)"));
                        meta.lore(List.of(
                                crossplayUtils.parseCrossplay(player, "&#555555Sigue explorando y farmeando"),
                                crossplayUtils.parseCrossplay(player, "&#555555para descubrir esta colección.")
                        ));
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    });
                } else {
                    item = new ItemStack(cItem.getIcono());
                    item.editMeta(meta -> {
                        meta.displayName(crossplayUtils.parseCrossplay(player, cItem.getNombre()));
                        meta.lore(List.of(
                                crossplayUtils.parseCrossplay(player, "&#E6CCFFNivel de Maestría: &#FFAA00" + nivelActual + " / " + cItem.getMaxTier()),
                                crossplayUtils.parseCrossplay(player, "&#E6CCFFProgreso Total: &#55FF55" + progreso),
                                net.kyori.adventure.text.Component.empty(),
                                crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para ver recompensas")
                        ));

                        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_item");
                        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, cItem.getId());
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    });
                }
                inventory.setItem(cItem.getSlotMenu(), item);
            }
            addBackButton("main");
        }
        // ==========================================
        // ⭐ 3. MENÚ DE TIERS (CAMINO DE MAESTRÍA S-SHAPE)
        // ==========================================
        else if (menuType == MenuType.ITEM_TIERS) {
            var cItem = collectionManager.getItemGlobal(itemId);
            if (cItem == null) return;

            var profile = collectionManager.getProfile(player.getUniqueId());
            int progreso = profile != null ? profile.getProgress(cItem.getId()) : 0;

            // 🌟 NUEVO: S-Shape Layout (Serpiente) para el inventario de 54 slots
            int[] tierSlots = {10, 12, 14, 16, 34, 32, 30, 28, 46, 48, 50, 52};
            int[] connectorSlots = {11, 13, 15, 25, 33, 31, 29, 37, 47, 49, 51};

            List<Tier> tiersOrdenados = new ArrayList<>(cItem.getTiers().values());
            tiersOrdenados.sort(Comparator.comparingInt(Tier::getNivel)); // Orden natural

            for (int i = 0; i < tiersOrdenados.size() && i < tierSlots.length; i++) {
                Tier tier = tiersOrdenados.get(i);
                int slot = tierSlots[i];
                RewardTemplate template = collectionManager.getRewardTemplate(tier.getRecompensaId());

                boolean desbloqueado = progreso >= tier.getRequerido();
                boolean reclamado = profile != null && profile.hasClaimedTier(cItem.getId(), tier.getNivel());

                // Base del Lore
                List<String> rawLore = new ArrayList<>();
                rawLore.add("&#555555------------------------");
                rawLore.add("&#E6CCFFProgreso: &#FFAA00" + progreso + "&#777777/&#FF5555" + tier.getRequerido());
                rawLore.add("");

                // Inyectar plantilla del Módulo 1
                if (template != null) {
                    rawLore.addAll(template.lore());
                    rawLore.add("");
                }

                ItemStack itemNode;

                if (reclamado) {
                    // NODO COMPLETADO
                    try {
                        itemNode = new ItemStack(cItem.getIcono());
                    } catch (Exception e) {
                        itemNode = new ItemStack(Material.PAPER);
                    }

                    itemNode.editMeta(meta -> {
                        meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55&l[✓] Nivel " + tier.getNivel() + " Completado"));
                        meta.addEnchant(Enchantment.UNBREAKING, 1, true); // Glow

                        List<String> tempLore = new ArrayList<>(rawLore);
                        tempLore.add("&#55FF55Ya has reclamado estas recompensas.");

                        List<net.kyori.adventure.text.Component> finalLoreComp = new ArrayList<>();
                        tempLore.forEach(line -> finalLoreComp.add(crossplayUtils.parseCrossplay(player, line)));
                        meta.lore(finalLoreComp);

                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                    });

                } else if (desbloqueado) {
                    // NODO DESBLOQUEADO (Listo para reclamar)
                    itemNode = new ItemStack(Material.EXPERIENCE_BOTTLE);
                    itemNode.editMeta(meta -> {
                        meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00&l[!] Nivel " + tier.getNivel() + " Desbloqueado"));
                        meta.addEnchant(Enchantment.UNBREAKING, 1, true);

                        List<String> tempLore = new ArrayList<>(rawLore);
                        tempLore.add("&#FFAA00▶ Haz clic para Reclamar Botín");

                        List<net.kyori.adventure.text.Component> finalLoreComp = new ArrayList<>();
                        tempLore.forEach(line -> finalLoreComp.add(crossplayUtils.parseCrossplay(player, line)));
                        meta.lore(finalLoreComp);

                        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "claim_tier");
                        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier.getNivel());
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                    });

                } else {
                    // NODO BLOQUEADO
                    itemNode = new ItemStack(Material.GRAY_DYE);
                    itemNode.editMeta(meta -> {
                        meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555&l[x] Nivel " + tier.getNivel() + " Bloqueado"));

                        List<String> tempLore = new ArrayList<>(rawLore);
                        tempLore.add("&#FF5555Sigue farmeando para desbloquear.");

                        List<net.kyori.adventure.text.Component> finalLoreComp = new ArrayList<>();
                        tempLore.forEach(line -> finalLoreComp.add(crossplayUtils.parseCrossplay(player, line)));
                        meta.lore(finalLoreComp);

                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    });
                }

                inventory.setItem(slot, itemNode);

                // 🔌 DIBUJAR CABLES (Cristales entre nodos)
                if (i < tiersOrdenados.size() - 1 && i < connectorSlots.length) {
                    Material glassMat = desbloqueado ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                    ItemStack cable = new ItemStack(glassMat);
                    cable.editMeta(meta -> {
                        meta.displayName(crossplayUtils.parseCrossplay(player, "&r"));
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    });
                    inventory.setItem(connectorSlots[i], cable);
                }
            }

            addBackButton("cat_" + cItem.getCategoriaId());

            // 🏆 Botón de Top 5
            var info = new ItemStack(Material.NETHER_STAR);
            info.editMeta(meta -> {
                meta.displayName(crossplayUtils.parseCrossplay(player, "&#ff00ff🏆 Ránking de Colección"));
                meta.lore(List.of(
                        crossplayUtils.parseCrossplay(player, "&#E6CCFFTu farmeo total: &#55FF55" + progreso),
                        net.kyori.adventure.text.Component.empty(),
                        crossplayUtils.parseCrossplay(player, "&#FFAA00▶ Haz clic para ver el TOP 5 Global")
                ));
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "show_top");
                meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, cItem.getId());
            });
            inventory.setItem(40, info);
        }
    }

    private void addBackButton(String target) {
        var back = new ItemStack(Material.ARROW);
        back.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555⬅ Volver Atrás"));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_" + target);
        });
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

        switch (action) {
            case "open_category" -> {
                String catId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                player.closeInventory();

                player.getScheduler().runDelayed(plugin, task -> {
                    new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, MenuType.CATEGORY, catId, "").open();
                }, null, 1L);
            }
            case "open_item" -> {
                String iId = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                player.closeInventory();

                player.getScheduler().runDelayed(plugin, task -> {
                    new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, MenuType.ITEM_TIERS, categoryId, iId).open();
                }, null, 1L);
            }
            case "claim_tier" -> {
                Integer tierNivel = meta.getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
                if (tierNivel != null) {
                    collectionManager.reclamarRecompensa(player, itemId, tierNivel);
                    setMenuItems(); // 🔄 Recargamos el menú de inmediato para que el nodo cambie a [✓] Completado
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

                    player.getScheduler().runDelayed(plugin, task -> {
                        if (target.equals("main")) {
                            new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, MenuType.MAIN, "", "").open();
                        } else if (target.startsWith("cat_")) {
                            new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, MenuType.CATEGORY, target.replace("cat_", ""), "").open();
                        }
                    }, null, 1L);
                }
            }
        }
    }
}
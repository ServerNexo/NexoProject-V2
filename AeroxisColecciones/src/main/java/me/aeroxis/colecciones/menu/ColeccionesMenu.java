package me.aeroxis.colecciones.menu;

import me.aeroxis.colecciones.AeroxisColecciones;
import me.aeroxis.core.menus.AeroxisMenu;
import me.aeroxis.colecciones.colecciones.CollectionManager;
import me.aeroxis.colecciones.data.CollectionCategory;
import me.aeroxis.colecciones.data.CollectionItem;
import me.aeroxis.colecciones.data.RewardTemplate;
import me.aeroxis.colecciones.data.Tier;
import me.aeroxis.core.crossplay.CrossplayUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 📚 AeroxisColecciones - Menú Interactivo (Arquitectura Enterprise Java 21)
 * Rendimiento: Paginación Dinámica Universal, Auto-Grid Layout, Cero Lag Visual.
 */
public class ColeccionesMenu extends AeroxisMenu {

    private final AeroxisColecciones plugin;
    private final CollectionManager collectionManager;
    private final CrossplayUtils crossplayUtils;

    private final MenuType menuType;
    private final String categoryId;
    private final String itemId;
    private final int page;

    private final NamespacedKey actionKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey itemKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey pageKey;

    public enum MenuType { MAIN, CATEGORY, ITEM_TIERS }

    public ColeccionesMenu(Player player, AeroxisColecciones plugin, CollectionManager collectionManager,
                           CrossplayUtils crossplayUtils, MenuType type, String categoryId, String itemId) {
        this(player, plugin, collectionManager, crossplayUtils, type, categoryId, itemId, 1);
    }

    public ColeccionesMenu(Player player, AeroxisColecciones plugin, CollectionManager collectionManager,
                           CrossplayUtils crossplayUtils, MenuType type, String categoryId, String itemId, int page) {
        super(player, crossplayUtils);

        this.plugin = plugin;
        this.collectionManager = collectionManager;
        this.crossplayUtils = crossplayUtils;

        this.menuType = type;
        this.categoryId = categoryId;
        this.itemId = itemId;
        this.page = page;

        this.actionKey = new NamespacedKey(plugin, "action");
        this.categoryKey = new NamespacedKey(plugin, "category_id");
        this.itemKey = new NamespacedKey(plugin, "item_id");
        this.tierKey = new NamespacedKey(plugin, "tier_level");
        this.pageKey = new NamespacedKey(plugin, "page_num");
    }

    @Override
    public String getMenuName() {
        if (menuType == MenuType.MAIN) return "&#FFAA00📚 <bold>TUS COLECCIONES</bold>";
        if (menuType == MenuType.CATEGORY) {
            var cat = collectionManager.getCategorias().get(categoryId);
            String catName = cat != null ? cat.getNombre() : "Categoría";
            return catName + " - Pág " + page; // 🌟 Agregado el número de página al título
        }

        var cItem = collectionManager.getItemGlobal(itemId);
        String name = (cItem != null) ? cItem.getNombre() : "Progreso";
        return "&#E6CCFF⭐ <bold>CAMINO: Pág " + page + "</bold>";
    }

    @Override
    public int getSlots() {
        return menuType == MenuType.MAIN ? 27 : 54; // 🌟 Ahora CATEGORY también usa 54 slots para caber más ítems
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
        // 📦 2. MENÚ DE CATEGORÍA (AUTO-GRID PAGINADO)
        // ==========================================
        else if (menuType == MenuType.CATEGORY) {
            var cat = collectionManager.getCategorias().get(categoryId);
            if (cat == null) return;

            var profile = collectionManager.getProfile(player.getUniqueId());

            // 🌟 NUEVO LAYOUT: Cuadrícula central de 28 slots por página
            int[] itemSlots = {
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
            };

            // Ordenamos los ítems por ID alfabéticamente para que no cambien de lugar
            List<CollectionItem> itemsOrdenados = new ArrayList<>(cat.getItems().values());
            itemsOrdenados.sort(Comparator.comparing(CollectionItem::getId));

            int maxPerPage = itemSlots.length; // 28
            int totalPages = Math.max(1, (int) Math.ceil(itemsOrdenados.size() / (double) maxPerPage));
            int startIndex = (page - 1) * maxPerPage;

            for (int i = 0; i < itemSlots.length; i++) {
                int itemIndex = startIndex + i;
                if (itemIndex >= itemsOrdenados.size()) break; // Si ya no hay ítems, paramos

                CollectionItem cItem = itemsOrdenados.get(itemIndex);
                int progreso = profile != null ? profile.getProgress(cItem.getId()) : 0;
                int nivelActual = collectionManager.calcularNivel(cItem, progreso);

                ItemStack item = new ItemStack(cItem.getIcono());
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

                // Lo ponemos en la cuadrícula, ignorando el YAML
                inventory.setItem(itemSlots[i], item);
            }

            addBackButton("main");

            // Botones de Navegación si hay muchos ítems
            if (page < totalPages) {
                var next = new ItemStack(Material.ARROW);
                next.editMeta(meta -> {
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00Siguiente Página ➡"));
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "change_page");
                    meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page + 1);
                });
                inventory.setItem(53, next);
            }
            if (page > 1) {
                var prev = new ItemStack(Material.ARROW);
                prev.editMeta(meta -> {
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00⬅ Página Anterior"));
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "change_page");
                    meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page - 1);
                });
                inventory.setItem(45, prev);
            }
        }
        // ==========================================
        // ⭐ 3. MENÚ DE TIERS (MONTAÑA RUSA VERTICAL)
        // ==========================================
        else if (menuType == MenuType.ITEM_TIERS) {
            var cItem = collectionManager.getItemGlobal(itemId);
            if (cItem == null) return;

            var profile = collectionManager.getProfile(player.getUniqueId());
            int progreso = profile != null ? profile.getProgress(cItem.getId()) : 0;

            int[] tierSlots = {10, 28, 38, 30, 12, 14, 32, 42, 34, 16};
            int[] connectorSlots = {19, 37, 39, 21, 13, 23, 41, 43, 25};

            List<Tier> tiersOrdenados = new ArrayList<>(cItem.getTiers().values());
            tiersOrdenados.sort(Comparator.comparingInt(Tier::getNivel));

            int maxPerPage = 10;
            int totalPages = Math.max(1, (int) Math.ceil(tiersOrdenados.size() / (double) maxPerPage));
            int startIndex = (page - 1) * maxPerPage;

            for (int i = 0; i < tierSlots.length; i++) {
                int tierIndex = startIndex + i;
                int slot = tierSlots[i];
                boolean isConfigured = tierIndex < tiersOrdenados.size();
                boolean isCurrentUnlocked = false;
                ItemStack itemNode;

                if (isConfigured) {
                    Tier tier = tiersOrdenados.get(tierIndex);
                    RewardTemplate template = collectionManager.getRewardTemplate(tier.getRecompensaId());

                    boolean desbloqueado = progreso >= tier.getRequerido();
                    boolean reclamado = profile != null && profile.hasClaimedTier(cItem.getId(), tier.getNivel());
                    isCurrentUnlocked = desbloqueado;

                    List<String> rawLore = new ArrayList<>();
                    rawLore.add("&#555555------------------------");
                    rawLore.add("&#E6CCFFProgreso: &#FFAA00" + progreso + "&#777777/&#FF5555" + tier.getRequerido());
                    rawLore.add("");

                    if (template != null) {
                        rawLore.addAll(template.lore());
                        rawLore.add("");
                    }

                    if (reclamado) {
                        try { itemNode = new ItemStack(cItem.getIcono()); }
                        catch (Exception e) { itemNode = new ItemStack(Material.PAPER); }

                        itemNode.editMeta(meta -> {
                            meta.displayName(crossplayUtils.parseCrossplay(player, "&#55FF55&l[✓] Nivel " + tier.getNivel() + " Completado"));
                            meta.addEnchant(Enchantment.UNBREAKING, 1, true);

                            List<String> tempLore = new ArrayList<>(rawLore);
                            tempLore.add("&#55FF55Ya has reclamado estas recompensas.");
                            List<net.kyori.adventure.text.Component> finalLoreComp = new ArrayList<>();
                            tempLore.forEach(line -> finalLoreComp.add(crossplayUtils.parseCrossplay(player, line)));
                            meta.lore(finalLoreComp);

                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                        });

                    } else if (desbloqueado) {
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

                } else {
                    itemNode = new ItemStack(Material.IRON_BARS);
                    itemNode.editMeta(meta -> {
                        meta.displayName(crossplayUtils.parseCrossplay(player, "&#555555&l[?] Nivel " + (tierIndex + 1)));
                        meta.lore(List.of(
                                crossplayUtils.parseCrossplay(player, "&#555555------------------------"),
                                crossplayUtils.parseCrossplay(player, "&#777777Próximamente..."),
                                crossplayUtils.parseCrossplay(player, "&#555555------------------------")
                        ));
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    });
                }

                inventory.setItem(slot, itemNode);

                if (i < connectorSlots.length) {
                    Material glassMat;
                    if (isConfigured && isCurrentUnlocked) {
                        glassMat = Material.LIME_STAINED_GLASS_PANE;
                    } else if (isConfigured) {
                        glassMat = Material.GRAY_STAINED_GLASS_PANE;
                    } else {
                        glassMat = Material.BLACK_STAINED_GLASS_PANE;
                    }

                    ItemStack cable = new ItemStack(glassMat);
                    cable.editMeta(meta -> {
                        meta.displayName(crossplayUtils.parseCrossplay(player, "&r"));
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    });
                    inventory.setItem(connectorSlots[i], cable);
                }
            }

            addBackButton("cat_" + cItem.getCategoriaId());

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
            inventory.setItem(50, info);

            if (page < totalPages) {
                var next = new ItemStack(Material.ARROW);
                next.editMeta(meta -> {
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00Siguiente Página ➡"));
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "change_page");
                    meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page + 1);
                });
                inventory.setItem(53, next);
            }

            if (page > 1) {
                var prev = new ItemStack(Material.ARROW);
                prev.editMeta(meta -> {
                    meta.displayName(crossplayUtils.parseCrossplay(player, "&#FFAA00⬅ Página Anterior"));
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "change_page");
                    meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page - 1);
                });
                inventory.setItem(45, prev);
            }
        }
    }

    private void addBackButton(String target) {
        var back = new ItemStack(Material.OAK_DOOR);
        back.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(player, "&#FF5555⬅ Volver Atrás"));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_" + target);
        });

        int slot = (getSlots() == 54) ? 49 : getSlots() - 5;
        inventory.setItem(slot, back);
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
                    setMenuItems();
                }
            }
            case "show_top" -> {
                String iId = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
                collectionManager.calcularTopAsync(player, iId);
            }
            case "change_page" -> {
                Integer newPage = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
                if (newPage != null) {
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
                    player.closeInventory();
                    player.getScheduler().runDelayed(plugin, task -> {
                        // 🌟 FIX CLAVE: Ahora pasa "this.menuType" para saber si cambias página de Tiers o de Categorías
                        new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, this.menuType, categoryId, itemId, newPage).open();
                    }, null, 1L);
                }
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
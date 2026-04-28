package me.nexo.chat.menu;

import me.nexo.chat.database.NexoChatDatabase;
import me.nexo.chat.managers.NexoChatManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 🏷️ Menú de Selección de Tags y Cosméticos (Data-Driven)
 */
public class TagsMenu extends NexoMenu {

    private final NexoChatManager chatManager;
    private final NexoChatDatabase chatDatabase;
    private final List<String> availableTags;
    private final List<String> tagsInMenuOrder = new ArrayList<>(); // Memoria temporal para saber en qué slot está cada Tag

    public TagsMenu(Player player, CrossplayUtils crossplayUtils, NexoChatManager chatManager, NexoChatDatabase chatDatabase) {
        super(player, crossplayUtils);
        this.chatManager = chatManager;
        this.chatDatabase = chatDatabase;

        Set<String> unlocked = chatManager.getUnlockedTagsMap().get(player.getUniqueId());
        this.availableTags = unlocked != null ? new ArrayList<>(unlocked) : new ArrayList<>();
    }

    @Override
    public String getMenuName() {
        return "§8🏷️ Tus Títulos y Tags";
    }

    @Override
    public int getSlots() {
        return 36; // 4 Filas
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // 🛑 Botón para quitarse el Tag
        setItem(4, Material.BARRIER, "§c§lQuitar Tag Activo", List.of(
                "§7Haz clic para limpiar tu nombre",
                "§7y no mostrar ningún icono extra."
        ));

        // 🏷️ Leer tags desde el archivo config.yml
        var config = Bukkit.getPluginManager().getPlugin("NexoChat").getConfig();
        var section = config.getConfigurationSection("tags_disponibles");
        if (section == null) return;

        String activeTagId = chatManager.getPlayerActiveTag(player.getUniqueId());
        int currentSlot = 10;

        tagsInMenuOrder.clear(); // Limpiamos la lista

        for (String tagId : section.getKeys(false)) {
            // Saltamos los bordes para centrar los íconos
            if (currentSlot == 17 || currentSlot == 18 || currentSlot == 26 || currentSlot == 27) currentSlot++;
            if (currentSlot >= 36) break; // Límite del menú

            boolean hasUnlocked = availableTags.contains(tagId);
            boolean isEquipped = tagId.equals(activeTagId);

            String menuName = config.getString("tags_disponibles." + tagId + ".menu_nombre", "&f" + tagId);
            String menuLore = config.getString("tags_disponibles." + tagId + ".menu_lore", "");
            String materialName = config.getString("tags_disponibles." + tagId + ".menu_material", "NAME_TAG");

            Material mat = Material.matchMaterial(materialName);
            if (mat == null) mat = Material.NAME_TAG;

            // Convertimos los componentes parseados a string legacy (colores de Bukkit §) para el NexoMenu
            String parsedName = LegacyComponentSerializer.legacySection().serialize(chatManager.parseColors(menuName));
            String parsedLore = LegacyComponentSerializer.legacySection().serialize(chatManager.parseColors(menuLore));

            if (hasUnlocked) {
                // 🔓 Lo tiene desbloqueado
                String status = isEquipped ? "§a(Equipado)" : "§e¡Clic para equipar!";
                setItem(currentSlot, mat, parsedName, List.of(parsedLore, "", status));
            } else {
                // 🔒 No lo tiene desbloqueado (Gris)
                setItem(currentSlot, Material.GRAY_DYE, "§8🔒 " + parsedName, List.of(
                        "§7No tienes este tag desbloqueado.",
                        parsedLore
                ));
            }

            // Guardamos el orden para saber a qué le hace clic luego
            tagsInMenuOrder.add(tagId);
            currentSlot++;
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(inventory)) return;

        // 🛑 Quitar Tag
        if (e.getSlot() == 4) {
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
            chatManager.setPlayerActiveTag(player.getUniqueId(), "");
            guardarAsync();
            player.sendMessage("§c🗑️ Te has quitado el Tag. Tu nombre vuelve a la normalidad.");
            player.closeInventory();
            return;
        }

        // 🏷️ Detectar clic en los Tags
        int clickedSlot = e.getSlot();
        int listIndex = -1;

        if (clickedSlot >= 10 && clickedSlot <= 16) listIndex = clickedSlot - 10;
        else if (clickedSlot >= 19 && clickedSlot <= 25) listIndex = clickedSlot - 19 + 7;

        if (listIndex >= 0 && listIndex < tagsInMenuOrder.size()) {
            String selectedTagId = tagsInMenuOrder.get(listIndex);

            // Verificamos si realmente lo tiene comprado/desbloqueado
            if (!availableTags.contains(selectedTagId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("§c🔒 Aún no has desbloqueado este tag.");
                return;
            }

            // Lo equipamos
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            chatManager.setPlayerActiveTag(player.getUniqueId(), selectedTagId);
            guardarAsync();

            player.sendMessage("§a✅ ¡Tag equipado correctamente!");
            open(); // Refrescamos el menú para que se actualice la vista
        }
    }

    // Guardado Asíncrono para no causar Lag Spikes en el servidor
    private void guardarAsync() {
        CompletableFuture.runAsync(() -> {
            // 🌟 FIX: Ya no mandamos el cosmeticTag, solo guardamos la info propia del Chat.
            chatDatabase.savePlayerData(player.getUniqueId(), chatManager.getIgnoredPlayersMap().get(player.getUniqueId()));
        });
    }
}
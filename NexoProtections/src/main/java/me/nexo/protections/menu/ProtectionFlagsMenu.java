package me.nexo.protections.menu;

import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.NexoMenu;
import me.nexo.core.utils.NexoColor;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ProtectionStone;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 🛡️ NexoProtections - Menú de Leyes del Dominio (Arquitectura Enterprise)
 * Rendimiento: Cero Streams, Llaves Cacheadas O(1) y Ensamblado Directo.
 */
public class ProtectionFlagsMenu extends NexoMenu {

    private final ProtectionStone stone;
    private final NexoProtections plugin;
    private final ConfigManager configManager;

    // 🌟 OPTIMIZACIÓN O(1): Cacheamos la llave en la memoria RAM
    private final NamespacedKey flagKey;

    public ProtectionFlagsMenu(Player player, NexoProtections plugin, ProtectionStone stone) {
        super(player);
        this.plugin = plugin;
        this.stone = stone;
        this.configManager = plugin.getConfigManager();

        this.flagKey = new NamespacedKey(plugin, "flag_id");
    }

    @Override
    public String getMenuName() {
        return configManager.getMessages().menus().leyes().titulo();
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        var flags = configManager.getMessages().menus().leyes().flags();

        // Fila 1: Entorno General
        createFlagItem(10, Material.NETHERITE_SWORD, flags.pvp(), "pvp");
        createFlagItem(11, Material.ZOMBIE_HEAD, flags.mobSpawning(), "mob-spawning");
        createFlagItem(12, Material.TNT, flags.tntDamage(), "tnt-damage");
        createFlagItem(13, Material.FLINT_AND_STEEL, flags.fireSpread(), "fire-spread");
        createFlagItem(14, Material.LEATHER, flags.animalDamage(), "animal-damage");

        // Fila 2: Interacciones de Forasteros
        createFlagItem(19, Material.OAK_DOOR, flags.interact(), "interact");
        createFlagItem(20, Material.CHEST, flags.containers(), "containers");
        createFlagItem(21, Material.HOPPER, flags.itemPickup(), "item-pickup");
        createFlagItem(22, Material.ROTTEN_FLESH, flags.itemDrop(), "item-drop");
        createFlagItem(23, Material.IRON_DOOR, flags.entry(), "ENTRY");

        // Botón Volver
        setItem(getSlots() - 5, Material.ENDER_PEARL, configManager.getMessages().menus().leyes().items().volver().nombre(), null);
    }

    /**
     * 🌟 Ensamblado Nativo: Crea el ítem, inyecta la llave y lo pone en el inventario
     * en una sola pasada, sin usar Streams lentos.
     */
    /**
     * 🌟 Ensamblado Nativo: Crea el ítem, inyecta la llave y lo pone en el inventario
     * en una sola pasada, sin usar Streams lentos.
     */
    private void createFlagItem(int slot, Material mat, String nombre, String flagId) {
        boolean activo = stone.getFlag(flagId);

        var flagNode = configManager.getMessages().menus().leyes().items().flag();
        String estadoColor = activo ? flagNode.estadoPermitido() : flagNode.estadoBloqueado();

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 🌟 FIX COLOR: Usamos el deserializador de Adventure que ya soporta Hexadecimales (&#RRGGBB) y códigos legacy (&a, &b)
            String nombreFormateado = "&#ff00ff<bold>" + nombre.toUpperCase() + "</bold>";
            // Reemplazamos los ampersand y códigos hex al formato que Kyori entiende si es necesario,
            // o usamos el legacySection si tus configs ya usan '§' o '&' internamente.
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(nombreFormateado.replace("&#", "&x&")));

            // Lore formateado sin usar Streams
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : flagNode.lore()) {
                String lineaFormateada = line.replace("%status%", estadoColor);
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(lineaFormateada.replace("&#", "&x&")));
            }
            meta.lore(lore);

            // Inyectamos la llave cacheada
            meta.getPersistentDataContainer().set(flagKey, PersistentDataType.STRING, flagId);

            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        event.setCancelled(true); // 🛑 Bloqueo absoluto contra robos

        // Ignorar si hace clic en su propio inventario
        if (event.getRawSlot() >= getSlots()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Clic en Volver
        if (clicked.getType() == Material.ENDER_PEARL) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            new ProtectionMenu(player, plugin, stone).open();
            return;
        }

        // Clic en Ley (Flag)
        ItemMeta meta = clicked.getItemMeta();

        if (meta.getPersistentDataContainer().has(flagKey, PersistentDataType.STRING)) {
            // Seguridad: Solo el dueño o un administrador pueden cambiar leyes
            if (!stone.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("nexoprotections.admin")) return;

            String flagId = meta.getPersistentDataContainer().get(flagKey, PersistentDataType.STRING);
            boolean actual = stone.getFlag(flagId);

            stone.setFlag(flagId, !actual); // Invierte la ley

            // 🛡️ FIX DESACOPLADO: Guardado directo al manager del mismo plugin (Cero saltos)
            plugin.getClaimManager().saveStoneDataAsync(stone);

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, 0.5f);

            // 🌟 MAGIA: Refresca el menú al instante, cambiando colores sin cerrar la ventana
            setMenuItems();
        }
    }
}
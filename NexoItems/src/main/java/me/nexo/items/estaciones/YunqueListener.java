package me.nexo.items.estaciones;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.EnchantDTO;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;

/**
 * 🎒 NexoItems - Yunque Omega para Encantamientos (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Region Sync, Async CompletableFutures y Cero Estáticos.
 */
@Singleton
public class YunqueListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final ItemManager itemManager;
    private final FileManager fileManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 LLAVES DESACOPLADAS O(1)
    private static final NamespacedKey WEAPON_ID_KEY = new NamespacedKey("nexoitems", "weapon_id");
    private static final NamespacedKey TOOL_ID_KEY = new NamespacedKey("nexoitems", "herramienta_id");
    private static final NamespacedKey ARMOR_ID_KEY = new NamespacedKey("nexoitems", "armadura_id");
    private static final NamespacedKey ENCHANT_ID_KEY = new NamespacedKey("nexoitems", "enchant_id");
    private static final NamespacedKey ENCHANT_LEVEL_KEY = new NamespacedKey("nexoitems", "enchant_nivel");

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class YunqueMenuHolder implements InventoryHolder {
        private final Inventory inventory;
        public YunqueMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public YunqueListener(NexoItems plugin, ItemManager itemManager, FileManager fileManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.fileManager = fileManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void abrirMenu(Player jugador) {
        // 🌟 Título Seguro a Component
        var tituloFormat = crossplayUtils.parseCrossplay(jugador, "&#a9a9a9🔨 <bold>YUNQUE OMEGA</bold>");

        var holder = new YunqueMenuHolder(tituloFormat);
        var inv = holder.getInventory();

        // 🌟 PAPER NATIVE: EditMeta directo
        var cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        cristal.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(jugador, " ")));

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        inv.setItem(11, new ItemStack(Material.AIR)); // Slot Activo
        inv.setItem(15, new ItemStack(Material.AIR)); // Slot Módulo/Libro

        var yunque = new ItemStack(Material.ANVIL);
        yunque.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(jugador, "&#00f5ff✨ <bold>FUSIONAR MÓDULO</bold>"));

            // 🌟 JAVA 21: List.of para inmutabilidad y rendimiento
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(jugador, "&#E6CCFFInserta un activo a la izquierda"),
                    crossplayUtils.parseCrossplay(jugador, "&#E6CCFFy un módulo de datos a la derecha."),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(jugador, "&#00f5ff► Clic para procesar")
            ));
        });
        inv.setItem(13, yunque);

        // 🛡️ FOLIA SYNC: Apertura en Hilo de Región
        jugador.getScheduler().run(plugin, task -> jugador.openInventory(inv), null);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof YunqueMenuHolder)) return;
        if (event.getClickedInventory() == null) return;

        var jugador = (Player) event.getWhoClicked();

        // 🛡️ PROTECCIÓN ANTI-DUPE (Shift-Click)
        if (event.isShiftClick() && event.getClickedInventory().equals(jugador.getInventory())) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(jugador, "&#FF5555⚠️ Utiliza el clic normal para colocar los objetos.");
            return;
        }

        int slot = event.getRawSlot();

        // Si hace clic en el menú superior (Yunque)
        if (event.getClickedInventory().equals(event.getInventory())) {
            if (slot != 11 && slot != 15 && slot != 13) {
                event.setCancelled(true);
                return;
            }

            // 🌟 LÓGICA DE FUSIÓN
            if (slot == 13) {
                event.setCancelled(true);
                var inv = event.getInventory();
                var itemObj = inv.getItem(11);
                var libro = inv.getItem(15);

                // 🌟 FIX GHOST ITEMS
                if (itemObj == null || itemObj.isEmpty()) {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Debes colocar un activo válido en la ranura izquierda.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                if (libro == null || libro.isEmpty() || !libro.hasItemMeta() || !libro.getItemMeta().getPersistentDataContainer().has(ENCHANT_ID_KEY, PersistentDataType.STRING)) {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Necesitas un Módulo de Encantamiento en la ranura derecha.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                var pdcItem = itemObj.getItemMeta().getPersistentDataContainer();
                boolean esArma = pdcItem.has(WEAPON_ID_KEY, PersistentDataType.STRING);
                boolean esHerramienta = pdcItem.has(TOOL_ID_KEY, PersistentDataType.STRING);
                boolean esArmadura = pdcItem.has(ARMOR_ID_KEY, PersistentDataType.STRING);

                if (!esArma && !esHerramienta && !esArmadura) {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Este objeto no soporta la integración de Módulos.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                String idEnchant = libro.getItemMeta().getPersistentDataContainer().get(ENCHANT_ID_KEY, PersistentDataType.STRING);
                int nivel = libro.getItemMeta().getPersistentDataContainer().getOrDefault(ENCHANT_LEVEL_KEY, PersistentDataType.INTEGER, 1);
                EnchantDTO enchantDTO = fileManager.getEnchantDTO(idEnchant);

                String tipoItem = esArma ? "Arma" : (esHerramienta ? "Herramienta" : "Armadura");

                if (enchantDTO != null && !enchantDTO.aplicaA().contains(tipoItem) && !enchantDTO.aplicaA().contains("Cualquiera")) {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Incompatibilidad: El módulo (" + enchantDTO.nombre() + ") no puede aplicarse a " + tipoItem + "s.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // 🛡️ PIPELINE ASÍNCRONO SEGURO: La generación y sincronización suceden en los Hilos Virtuales
                // Y regresamos la respuesta para aplicarla en el inventario del menú (Hilo Principal - Región)
                var itemInicial = itemManager.aplicarEncantamiento(itemObj, idEnchant, nivel);
                
                itemManager.sincronizarItemAsync(itemInicial).thenAccept(itemTerminado -> {
                    // 🛡️ FOLIA SYNC: Aplica visualmente el ítem terminado en el hilo de la región
                    jugador.getScheduler().run(plugin, task -> {
                        inv.setItem(11, itemTerminado);
                        inv.setItem(15, new ItemStack(Material.AIR));

                        String nombreEncanto = enchantDTO != null ? enchantDTO.nombre() : "Módulo Desconocido";
                        crossplayUtils.sendMessage(jugador, "&#55FF55[✓] Fusión Exitosa. Se ha integrado el módulo: &#00E5FF" + nombreEncanto);
                        jugador.playSound(jugador.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
                    }, null);
                });
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof YunqueMenuHolder)) return;

        var jugador = (Player) event.getPlayer();
        var inv = event.getInventory();

        var item = inv.getItem(11);
        var libro = inv.getItem(15);

        // 🛡️ PROTECCIÓN ANTI-VOID
        HashMap<Integer, ItemStack> sobrantes = new HashMap<>();

        if (item != null && !item.isEmpty()) {
            sobrantes.putAll(jugador.getInventory().addItem(item));
        }
        if (libro != null && !libro.isEmpty()) {
            sobrantes.putAll(jugador.getInventory().addItem(libro));
        }

        for (var drop : sobrantes.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
        }
    }

    // 🔧 Previene que los jugadores cambien el nombre de ítems RPG en el yunque vanilla
    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        var item1 = event.getInventory().getItem(0);
        var result = event.getResult();

        if (item1 != null && !item1.isEmpty() && item1.hasItemMeta() && result != null && !result.isEmpty() && result.hasItemMeta()) {
            var meta1 = item1.getItemMeta();
            var pdc = meta1.getPersistentDataContainer();

            if (pdc.has(WEAPON_ID_KEY, PersistentDataType.STRING) ||
                pdc.has(TOOL_ID_KEY, PersistentDataType.STRING) ||
                pdc.has(ARMOR_ID_KEY, PersistentDataType.STRING)) {

                String oldName = PlainTextComponentSerializer.plainText().serialize(meta1.displayName());
                String newName = PlainTextComponentSerializer.plainText().serialize(result.getItemMeta().displayName());

                if (!oldName.equals(newName)) {
                    event.setResult(null); // Bloquea el cambio de nombre
                }
            }
        }
    }
}
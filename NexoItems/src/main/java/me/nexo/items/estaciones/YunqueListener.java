package me.nexo.items.estaciones;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.EnchantDTO;
import me.nexo.items.managers.ItemManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🎒 NexoItems - Yunque Omega para Encantamientos (Arquitectura Enterprise)
 */
@Singleton
public class YunqueListener implements Listener {

    private final NexoItems plugin;

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class YunqueMenuHolder implements InventoryHolder {
        private final Inventory inventory;
        public YunqueMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public YunqueListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player jugador) {
        // 🌟 Título Seguro a Component
        net.kyori.adventure.text.Component tituloFormat = CrossplayUtils.parseCrossplay(jugador, "&#a9a9a9🔨 <bold>YUNQUE OMEGA</bold>");

        YunqueMenuHolder holder = new YunqueMenuHolder(tituloFormat);
        Inventory inv = holder.getInventory();

        ItemStack cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta metaCristal = cristal.getItemMeta();
        if (metaCristal != null) {
            metaCristal.displayName(CrossplayUtils.parseCrossplay(jugador, " "));
            cristal.setItemMeta(metaCristal);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        inv.setItem(11, new ItemStack(Material.AIR)); // Slot Activo
        inv.setItem(15, new ItemStack(Material.AIR)); // Slot Módulo/Libro

        ItemStack yunque = new ItemStack(Material.ANVIL);
        ItemMeta metaYunque = yunque.getItemMeta();
        if (metaYunque != null) {
            metaYunque.displayName(CrossplayUtils.parseCrossplay(jugador, "&#00f5ff✨ <bold>FUSIONAR MÓDULO</bold>"));

            // 🌟 Lore compatible con Bedrock y Spigot
            List<String> loreRaw = List.of(
                    "&#E6CCFFInserta un activo a la izquierda",
                    "&#E6CCFFy un módulo de datos a la derecha.",
                    "",
                    "&#00f5ff► Clic para procesar"
            );
            List<String> loreFormateado = loreRaw.stream()
                    .map(line -> LegacyComponentSerializer.legacySection().serialize(me.nexo.core.utils.NexoColor.parse(line)))
                    .collect(Collectors.toList());
            metaYunque.setLore(loreFormateado);

            yunque.setItemMeta(metaYunque);
        }
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable (Adiós validación por títulos)
        if (!(event.getInventory().getHolder() instanceof YunqueMenuHolder)) return;
        if (event.getClickedInventory() == null) return;

        Player jugador = (Player) event.getWhoClicked();

        // 🛡️ PROTECCIÓN ANTI-DUPE (Shift-Click)
        if (event.isShiftClick() && event.getClickedInventory().equals(jugador.getInventory())) {
            event.setCancelled(true);
            CrossplayUtils.sendMessage(jugador, "&#FF5555⚠️ Utiliza el clic normal para colocar los objetos.");
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
                Inventory inv = event.getInventory();
                ItemStack itemObj = inv.getItem(11);
                ItemStack libro = inv.getItem(15);

                if (itemObj == null || itemObj.getType() == Material.AIR) {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Debes colocar un activo válido en la ranura izquierda.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                if (libro == null || !libro.hasItemMeta() || !libro.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveEnchantId, PersistentDataType.STRING)) {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Necesitas un Módulo de Encantamiento en la ranura derecha.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                var pdcItem = itemObj.getItemMeta().getPersistentDataContainer();
                boolean esArma = pdcItem.has(ItemManager.llaveWeaponId, PersistentDataType.STRING);
                boolean esHerramienta = pdcItem.has(ItemManager.llaveHerramientaId, PersistentDataType.STRING);
                boolean esArmadura = pdcItem.has(ItemManager.llaveArmaduraId, PersistentDataType.STRING);

                if (!esArma && !esHerramienta && !esArmadura) {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Este objeto no soporta la integración de Módulos.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                String idEnchant = libro.getItemMeta().getPersistentDataContainer().get(ItemManager.llaveEnchantId, PersistentDataType.STRING);
                int nivel = libro.getItemMeta().getPersistentDataContainer().getOrDefault(ItemManager.llaveEnchantNivel, PersistentDataType.INTEGER, 1);
                EnchantDTO enchantDTO = plugin.getFileManager().getEnchantDTO(idEnchant);

                String tipoItem = esArma ? "Arma" : (esHerramienta ? "Herramienta" : "Armadura");

                if (enchantDTO != null && !enchantDTO.aplicaA().contains(tipoItem) && !enchantDTO.aplicaA().contains("Cualquiera")) {
                    CrossplayUtils.sendMessage(jugador, "&#FF5555[!] Incompatibilidad: El módulo (" + enchantDTO.nombre() + ") no puede aplicarse a " + tipoItem + "s.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // Aplicar el encantamiento
                ItemStack itemEncantado = ItemManager.aplicarEncantamiento(itemObj, idEnchant, nivel);
                ItemManager.sincronizarItemAsync(itemEncantado);

                inv.setItem(11, itemEncantado);
                inv.setItem(15, new ItemStack(Material.AIR));

                String nombreEncanto = enchantDTO != null ? enchantDTO.nombre() : "Módulo Desconocido";
                CrossplayUtils.sendMessage(jugador, "&#55FF55[✓] Fusión Exitosa. Se ha integrado el módulo: &#00E5FF" + nombreEncanto);
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof YunqueMenuHolder)) return;

        Player jugador = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        ItemStack item = inv.getItem(11);
        ItemStack libro = inv.getItem(15);

        // 🛡️ PROTECCIÓN ANTI-VOID (Rescata ítems sobrantes si el inventario está lleno)
        HashMap<Integer, ItemStack> sobrantes = new HashMap<>();

        if (item != null && item.getType() != Material.AIR) {
            sobrantes.putAll(jugador.getInventory().addItem(item));
        }
        if (libro != null && libro.getType() != Material.AIR) {
            sobrantes.putAll(jugador.getInventory().addItem(libro));
        }

        for (ItemStack drop : sobrantes.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
        }
    }

    // 🔧 Previene que los jugadores cambien el nombre de ítems RPG en el yunque vanilla
    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack item1 = event.getInventory().getItem(0);
        ItemStack result = event.getResult();

        if (item1 != null && item1.hasItemMeta() && result != null && result.hasItemMeta()) {
            ItemMeta meta1 = item1.getItemMeta();

            if (meta1.getPersistentDataContainer().has(ItemManager.llaveWeaponId, PersistentDataType.STRING) ||
                    meta1.getPersistentDataContainer().has(ItemManager.llaveHerramientaId, PersistentDataType.STRING) ||
                    meta1.getPersistentDataContainer().has(ItemManager.llaveArmaduraId, PersistentDataType.STRING)) {

                String oldName = PlainTextComponentSerializer.plainText().serialize(meta1.displayName());
                String newName = PlainTextComponentSerializer.plainText().serialize(result.getItemMeta().displayName());

                if (!oldName.equals(newName)) {
                    event.setResult(null); // Bloquea el cambio de nombre
                }
            }
        }
    }
}
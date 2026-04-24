package me.nexo.items.estaciones;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.dtos.ReforgeDTO;
import me.nexo.items.dtos.ToolDTO;
import me.nexo.items.dtos.WeaponDTO;
import me.nexo.items.managers.FileManager;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 🎒 NexoItems - Mesa de Reforjas Aleatorias (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Region Sync, ThreadLocalRandom, EditMeta O(1) y Cero Estáticos.
 */
@Singleton
public class ReforjaListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final FileManager fileManager;
    private final ItemManager itemManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 LLAVES DESACOPLADAS O(1)
    private static final NamespacedKey MATERIAL_UPGRADE_KEY = new NamespacedKey("nexoitems", "nexo_material_polvo");
    private static final NamespacedKey WEAPON_ID_KEY = new NamespacedKey("nexoitems", "weapon_id");
    private static final NamespacedKey TOOL_ID_KEY = new NamespacedKey("nexoitems", "herramienta_id");

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class ReforjaMenuHolder implements InventoryHolder {
        private final Inventory inventory;
        public ReforjaMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ReforjaListener(NexoItems plugin, FileManager fileManager, ItemManager itemManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.fileManager = fileManager;
        this.itemManager = itemManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void abrirMenu(Player jugador) {
        // 🌟 Título Seguro a Component
        var tituloFormat = crossplayUtils.parseCrossplay(jugador, "&#ff00ff🔮 <bold>MESA DE REFORJA</bold>");

        var holder = new ReforjaMenuHolder(tituloFormat);
        var inv = holder.getInventory();

        // 🌟 PAPER NATIVE: EditMeta
        var cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        cristal.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(jugador, " ")));

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        inv.setItem(11, new ItemStack(Material.AIR)); // Slot Arma
        inv.setItem(15, new ItemStack(Material.AIR)); // Slot Material

        var yunque = new ItemStack(Material.SMITHING_TABLE);
        yunque.editMeta(meta -> {
            meta.displayName(crossplayUtils.parseCrossplay(jugador, "&#00f5ff✨ <bold>ALTERAR MATRIZ (REFORJAR)</bold>"));

            // 🌟 JAVA 21: Lista inmutable y renderizado directo Kyori Component
            meta.lore(List.of(
                    crossplayUtils.parseCrossplay(jugador, "&#E6CCFFAltera aleatoriamente los"),
                    crossplayUtils.parseCrossplay(jugador, "&#E6CCFFatributos base de tu activo."),
                    net.kyori.adventure.text.Component.empty(),
                    crossplayUtils.parseCrossplay(jugador, "&#FF3366[!] La reforja anterior se perderá."),
                    crossplayUtils.parseCrossplay(jugador, "&#00f5ff► Clic para procesar")
            ));
        });
        inv.setItem(13, yunque);

        // 🛡️ FOLIA SYNC: Apertura
        jugador.getScheduler().run(plugin, task -> jugador.openInventory(inv), null);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof ReforjaMenuHolder)) return;
        if (event.getClickedInventory() == null) return;

        var jugador = (Player) event.getWhoClicked();

        // 🛡️ PROTECCIÓN ANTI-DUPE (Shift-Click bloqueado)
        if (event.isShiftClick() && event.getClickedInventory().equals(jugador.getInventory())) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(jugador, "&#FF5555⚠️ Utiliza el clic normal para colocar los objetos.");
            return;
        }

        int slot = event.getRawSlot();

        // Si hace clic en el menú superior
        if (event.getClickedInventory().equals(event.getInventory())) {
            if (slot != 11 && slot != 15 && slot != 13) {
                event.setCancelled(true);
                return;
            }

            // 🌟 LÓGICA DE REFORJA
            if (slot == 13) {
                event.setCancelled(true);
                var inv = event.getInventory();
                var arma = inv.getItem(11);
                var material = inv.getItem(15);

                // 🌟 FIX GHOST ITEMS
                if (arma == null || arma.isEmpty()) {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Inserta un activo válido en la ranura izquierda.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                if (material == null || material.isEmpty() || !material.hasItemMeta() ||
                        !material.getItemMeta().getPersistentDataContainer().has(MATERIAL_UPGRADE_KEY, PersistentDataType.BYTE)) {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Necesitas Polvo de Mejora en la ranura derecha.");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                var pdc = arma.getItemMeta().getPersistentDataContainer();

                boolean esArma = pdc.has(WEAPON_ID_KEY, PersistentDataType.STRING);
                boolean esHerramienta = pdc.has(TOOL_ID_KEY, PersistentDataType.STRING);

                if (!esArma && !esHerramienta) {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] Este activo no soporta modificaciones de matriz (Reforja).");
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                String claseItem = "Cualquiera";
                if (esArma) {
                    WeaponDTO armaDto = fileManager.getWeaponDTO(pdc.get(WEAPON_ID_KEY, PersistentDataType.STRING));
                    if (armaDto != null) claseItem = armaDto.claseRequerida();
                } else {
                    ToolDTO toolDto = fileManager.getToolDTO(pdc.get(TOOL_ID_KEY, PersistentDataType.STRING));
                    if (toolDto != null) claseItem = toolDto.profesion();
                }

                // Filtrar reforjas compatibles
                List<ReforgeDTO> reforjasCompatibles = new ArrayList<>();
                var reforjasSection = fileManager.getReforjas().getConfigurationSection("reforjas");

                if (reforjasSection != null) {
                    for (String key : reforjasSection.getKeys(false)) {
                        ReforgeDTO dto = fileManager.getReforgeDTO(key);
                        if (dto != null && (dto.aplicaAClase(claseItem) || dto.aplicaAClase("Cualquiera"))) {
                            reforjasCompatibles.add(dto);
                        }
                    }
                }

                if (reforjasCompatibles.isEmpty()) {
                    crossplayUtils.sendMessage(jugador, "&#FF5555[!] El sistema no detecta reforjas compatibles para la clase: " + claseItem);
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // Consumir material
                material.setAmount(material.getAmount() - 1);

                // Tirada RNG Thread-Safe
                ReforgeDTO reforjaElegida = reforjasCompatibles.get(ThreadLocalRandom.current().nextInt(reforjasCompatibles.size()));

                // 🌟 FIX: Aplicación Síncrona Directa (Sin thenAccept)
                ItemStack armaReforjada = itemManager.aplicarReforja(arma, reforjaElegida.id());

                inv.setItem(11, armaReforjada);
                crossplayUtils.sendMessage(jugador, "&#55FF55[✓] Matriz Alterada. Nuevo prefijo: " + reforjaElegida.prefijoColor() + reforjaElegida.nombre());
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReforjaMenuHolder)) return;

        var jugador = (Player) event.getPlayer();
        var inv = event.getInventory();

        var arma = inv.getItem(11);
        var material = inv.getItem(15);

        // 🛡️ PROTECCIÓN ANTI-VOID
        HashMap<Integer, ItemStack> sobrantes = new HashMap<>();

        if (arma != null && !arma.isEmpty()) {
            sobrantes.putAll(jugador.getInventory().addItem(arma));
        }
        if (material != null && !material.isEmpty()) {
            sobrantes.putAll(jugador.getInventory().addItem(material));
        }

        for (var drop : sobrantes.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
        }
    }
}
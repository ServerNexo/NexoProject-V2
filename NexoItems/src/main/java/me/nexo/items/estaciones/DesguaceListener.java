package me.nexo.items.estaciones;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.config.ConfigManager;
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

import java.util.HashMap;

/**
 * 🎒 NexoItems - Desguace de Artefactos (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Region Sync, Anti-Ghost Items y Cero Dependencias Estáticas.
 */
@Singleton
public class DesguaceListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final ItemManager itemManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 LLAVE DESACOPLADA O(1)
    private static final NamespacedKey UPGRADE_LEVEL_KEY = new NamespacedKey("nexoitems", "nexo_upgrade");

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class DesguaceMenuHolder implements InventoryHolder {
        private final Inventory inventory;

        public DesguaceMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public DesguaceListener(NexoItems plugin, ItemManager itemManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void abrirMenu(Player jugador) {
        // Leemos desde la nueva arquitectura
        String rawTitle = configManager.getMessages().menus().desguace().titulo();
        var tituloFormat = crossplayUtils.parseCrossplay(jugador, rawTitle);

        var holder = new DesguaceMenuHolder(tituloFormat);
        var inv = holder.getInventory();

        // 🌟 PAPER NATIVE: EditMeta directo
        var cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        cristal.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(jugador, " ")));

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        inv.setItem(11, new ItemStack(Material.AIR));

        // Botón Central
        var btn = new ItemStack(Material.LAVA_BUCKET);
        btn.editMeta(meta -> {
            String btnTitle = configManager.getMessages().menus().desguace().boton().titulo();
            var btnLore = configManager.getMessages().menus().desguace().boton().lore();

            meta.displayName(crossplayUtils.parseCrossplay(jugador, btnTitle));
            meta.lore(btnLore.stream().map(line -> crossplayUtils.parseCrossplay(jugador, line)).toList());
        });
        inv.setItem(15, btn);

        // 🛡️ FOLIA SYNC: Apertura
        jugador.getScheduler().run(plugin, task -> jugador.openInventory(inv), null);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof DesguaceMenuHolder)) return;
        if (event.getClickedInventory() == null) return;

        var jugador = (Player) event.getWhoClicked();

        // 🛡️ PROTECCIÓN ANTI-DUPE (Evita mover items raros con Shift)
        if (event.isShiftClick() && event.getClickedInventory().equals(jugador.getInventory())) {
            event.setCancelled(true);
            // 🌟 FIX: Adiós '§c' legacy.
            crossplayUtils.sendMessage(jugador, "&#FF5555⚠️ Utiliza el clic normal para colocar los artefactos.");
            return;
        }

        int slot = event.getRawSlot();

        // Si hace clic en el menú superior (Desguace)
        if (event.getClickedInventory().equals(event.getInventory())) {

            if (slot != 11 && slot != 15) {
                event.setCancelled(true);
                return;
            }

            // 🌟 LÓGICA DEL BOTÓN DE DESGUACE
            if (slot == 15) {
                event.setCancelled(true);
                var inv = event.getInventory();
                var arma = inv.getItem(11);

                var nodos = configManager.getMessages().estaciones();

                // 🌟 FIX GHOST ITEMS
                if (arma == null || arma.isEmpty()) {
                    crossplayUtils.sendMessage(jugador, nodos.insertaActivo());
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                var pdc = arma.getItemMeta().getPersistentDataContainer();
                if (!pdc.has(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER)) {
                    crossplayUtils.sendMessage(jugador, nodos.noSoportaMejoras()); // Reutilizamos el nodo de error
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                int nivel = pdc.getOrDefault(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, 0);
                int cantidadPolvo = 1 + nivel; // Recupera base + nivel de mejora

                // Destruimos el arma
                inv.setItem(11, new ItemStack(Material.AIR));

                // 🌟 USO DE DEPENDENCIA INYECTADA
                var recompensa = itemManager.crearPolvoEstelar(); 
                if (recompensa != null && !recompensa.isEmpty()) {
                    recompensa.setAmount(cantidadPolvo);
                    HashMap<Integer, ItemStack> sobrante = jugador.getInventory().addItem(recompensa);

                    // Si no cabe en el inventario, lo tiramos al suelo
                    for (var drop : sobrante.values()) {
                        jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
                    }
                }

                crossplayUtils.sendMessage(jugador, nodos.desguaceExitoso().replace("%amount%", String.valueOf(cantidadPolvo)));
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DesguaceMenuHolder)) return;

        var jugador = (Player) event.getPlayer();
        var arma = event.getInventory().getItem(11);

        // 🛡️ PROTECCIÓN ANTI-VOID
        if (arma != null && !arma.isEmpty()) {
            HashMap<Integer, ItemStack> sobrantes = jugador.getInventory().addItem(arma);
            for (var drop : sobrantes.values()) {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
            }
        }
    }
}
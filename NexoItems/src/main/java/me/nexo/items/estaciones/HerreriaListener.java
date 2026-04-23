package me.nexo.items.estaciones;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.config.ConfigManager;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 🎒 NexoItems - Herrería de Mejoras (Arquitectura Enterprise Java 21)
 * Rendimiento: Folia Region Sync, O(1) Metadatos, ThreadLocalRandom y Cero Estáticos.
 */
@Singleton
public class HerreriaListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 LLAVES DESACOPLADAS O(1)
    private static final NamespacedKey MATERIAL_UPGRADE_KEY = new NamespacedKey("nexoitems", "nexo_material_polvo");
    private static final NamespacedKey UPGRADE_LEVEL_KEY = new NamespacedKey("nexoitems", "nexo_upgrade");

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class HerreriaMenuHolder implements InventoryHolder {
        private final Inventory inventory;

        public HerreriaMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public HerreriaListener(NexoItems plugin, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    public void abrirMenu(Player jugador) {
        // Leemos desde la arquitectura inyectada
        String rawTitle = configManager.getMessages().menus().herreria().titulo();
        var tituloFormat = crossplayUtils.parseCrossplay(jugador, rawTitle);

        var holder = new HerreriaMenuHolder(tituloFormat);
        var inv = holder.getInventory();

        // 🌟 PAPER NATIVE: EditMeta sin generar basura
        var cristal = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        cristal.editMeta(meta -> meta.displayName(crossplayUtils.parseCrossplay(jugador, " ")));

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, cristal);
        }

        inv.setItem(11, new ItemStack(Material.AIR)); // Hueco Arma
        inv.setItem(15, new ItemStack(Material.AIR)); // Hueco Material

        var yunque = new ItemStack(Material.ANVIL);
        yunque.editMeta(meta -> {
            String btnTitle = configManager.getMessages().menus().herreria().boton().titulo();
            var btnLore = configManager.getMessages().menus().herreria().boton().lore();

            meta.displayName(crossplayUtils.parseCrossplay(jugador, btnTitle));
            meta.lore(btnLore.stream().map(line -> crossplayUtils.parseCrossplay(jugador, line)).toList());
        });
        inv.setItem(13, yunque);

        // 🛡️ FOLIA SYNC: Apertura en Hilo de Región
        jugador.getScheduler().run(plugin, task -> jugador.openInventory(inv), null);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof HerreriaMenuHolder)) return;

        if (event.getClickedInventory() == null) return;
        var jugador = (Player) event.getWhoClicked();

        // 🛡️ PROTECCIÓN ANTI-DUPE (Evita mover items raros con Shift)
        if (event.isShiftClick() && event.getClickedInventory().equals(jugador.getInventory())) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(jugador, "&#FF5555⚠️ Utiliza el clic normal para colocar los artefactos.");
            return;
        }

        int slot = event.getRawSlot();

        // Si hace clic en el menú superior (Herrería)
        if (event.getClickedInventory().equals(event.getInventory())) {

            // Si hace clic en los cristales, cancelamos
            if (slot != 11 && slot != 15 && slot != 13) {
                event.setCancelled(true);
                return;
            }

            // 🌟 LÓGICA DEL BOTÓN DE FORJA
            if (slot == 13) {
                event.setCancelled(true);
                var inv = event.getInventory();
                var arma = inv.getItem(11);
                var material = inv.getItem(15);

                var nodos = configManager.getMessages().estaciones();

                // 🌟 FIX GHOST ITEMS
                if (arma == null || arma.isEmpty()) {
                    crossplayUtils.sendMessage(jugador, nodos.insertaActivo());
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                if (material == null || material.isEmpty() || !material.hasItemMeta() ||
                        !material.getItemMeta().getPersistentDataContainer().has(MATERIAL_UPGRADE_KEY, PersistentDataType.BYTE)) {
                    crossplayUtils.sendMessage(jugador, nodos.necesitasPolvo());
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // Editamos el arma in-place (O(1))
                arma.editMeta(meta -> {
                    if (!meta.getPersistentDataContainer().has(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER)) {
                        crossplayUtils.sendMessage(jugador, nodos.noSoportaMejoras());
                        return;
                    }

                    int nivelActual = meta.getPersistentDataContainer().getOrDefault(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, 0);

                    if (nivelActual >= 10) {
                        crossplayUtils.sendMessage(jugador, nodos.mejoraMaxima());
                        return;
                    }

                    // Consumimos el material visualmente (Se procesa al instante porque esto corre síncrono)
                    material.setAmount(material.getAmount() - 1);

                    // Tirada RNG Thread-Safe
                    int chanceExito = 100 - (nivelActual * 10);
                    int tiro = ThreadLocalRandom.current().nextInt(1, 101);

                    if (tiro <= chanceExito) {
                        final int nuevoNivel = nivelActual + 1;
                        meta.getPersistentDataContainer().set(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, nuevoNivel);

                        String nombreViejo = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                        String nombreNuevo = nombreViejo.replaceAll("\\[\\+\\d+\\]", "[+" + nuevoNivel + "]");
                        meta.displayName(crossplayUtils.parseCrossplay(jugador, nombreNuevo));

                        crossplayUtils.sendMessage(jugador, nodos.mejoraExitosa().replace("%level%", String.valueOf(nuevoNivel)));
                        jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    } else {
                        crossplayUtils.sendMessage(jugador, nodos.mejoraFallida());
                        jugador.playSound(jugador.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    }
                });
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof HerreriaMenuHolder)) return;

        var jugador = (Player) event.getPlayer();
        var inv = event.getInventory();

        var arma = inv.getItem(11);
        var material = inv.getItem(15);

        // 🛡️ PROTECCIÓN ANTI-VOID (Si el inventario está lleno, lo tira al suelo)
        HashMap<Integer, ItemStack> sobrantes = new HashMap<>();

        if (arma != null && !arma.isEmpty()) {
            sobrantes.putAll(jugador.getInventory().addItem(arma));
        }
        if (material != null && !material.isEmpty()) {
            sobrantes.putAll(jugador.getInventory().addItem(material));
        }

        // Escupimos los ítems que no cupieron al suelo real
        for (var drop : sobrantes.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
        }
    }
}
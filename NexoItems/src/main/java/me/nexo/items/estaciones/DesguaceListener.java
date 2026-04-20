package me.nexo.items.estaciones;

import com.google.inject.Inject;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class DesguaceListener implements Listener {

    private final NexoItems plugin;

    // 🌟 FIX: Añadimos @Inject para que Guice sepa cómo construir esta clase
    @Inject
    public DesguaceListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class DesguaceMenuHolder implements InventoryHolder {
        private final Inventory inventory;

        // 🌟 Recibe Component directamente
        public DesguaceMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    public void abrirMenu(Player jugador) {
        // Leemos desde la nueva arquitectura y parseamos a Component
        String rawTitle = plugin.getConfigManager().getMessages().menus().desguace().titulo();
        net.kyori.adventure.text.Component tituloFormat = CrossplayUtils.parseCrossplay(jugador, rawTitle);

        DesguaceMenuHolder holder = new DesguaceMenuHolder(tituloFormat);
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

        inv.setItem(11, new ItemStack(Material.AIR));

        // Botón Central
        ItemStack btn = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta btnMeta = btn.getItemMeta();
        if (btnMeta != null) {
            String btnTitle = plugin.getConfigManager().getMessages().menus().desguace().boton().titulo();
            List<String> btnLore = plugin.getConfigManager().getMessages().menus().desguace().boton().lore();

            btnMeta.displayName(CrossplayUtils.parseCrossplay(jugador, btnTitle));
            btnMeta.lore(btnLore.stream().map(line -> CrossplayUtils.parseCrossplay(jugador, line)).collect(Collectors.toList()));
            btn.setItemMeta(btnMeta);
        }
        inv.setItem(15, btn);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof DesguaceMenuHolder)) return;
        if (event.getClickedInventory() == null) return;

        Player jugador = (Player) event.getWhoClicked();

        // 🛡️ PROTECCIÓN ANTI-DUPE (Evita mover items raros con Shift)
        if (event.isShiftClick() && event.getClickedInventory().equals(jugador.getInventory())) {
            event.setCancelled(true);
            jugador.sendMessage("§c⚠️ Utiliza el clic normal para colocar los artefactos.");
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
                Inventory inv = event.getInventory();
                ItemStack arma = inv.getItem(11);

                var nodos = plugin.getConfigManager().getMessages().estaciones();

                if (arma == null || arma.getType() == Material.AIR) {
                    CrossplayUtils.sendMessage(jugador, nodos.insertaActivo());
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                if (!arma.hasItemMeta() || !arma.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                    CrossplayUtils.sendMessage(jugador, nodos.noSoportaMejoras()); // Reutilizamos el nodo de error
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                int nivel = arma.getItemMeta().getPersistentDataContainer().getOrDefault(ItemManager.llaveNivelMejora, PersistentDataType.INTEGER, 0);
                int cantidadPolvo = 1 + nivel; // Recupera base + nivel de mejora

                // Destruimos el arma
                inv.setItem(11, new ItemStack(Material.AIR));

                // Entregamos la recompensa
                ItemStack recompensa = ItemManager.crearPolvoEstelar(); // Asumo que este método existe en tu ItemManager
                if (recompensa != null) {
                    recompensa.setAmount(cantidadPolvo);
                    HashMap<Integer, ItemStack> sobrante = jugador.getInventory().addItem(recompensa);

                    // Si no cabe en el inventario, lo tiramos al suelo
                    for (ItemStack drop : sobrante.values()) {
                        jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
                    }
                }

                CrossplayUtils.sendMessage(jugador, nodos.desguaceExitoso().replace("%amount%", String.valueOf(cantidadPolvo)));
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DesguaceMenuHolder)) return;

        Player jugador = (Player) event.getPlayer();
        ItemStack arma = event.getInventory().getItem(11);

        // 🛡️ PROTECCIÓN ANTI-VOID (Si el inventario está lleno, cae al suelo)
        if (arma != null && arma.getType() != Material.AIR) {
            HashMap<Integer, ItemStack> sobrantes = jugador.getInventory().addItem(arma);
            for (ItemStack drop : sobrantes.values()) {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
            }
        }
    }
}
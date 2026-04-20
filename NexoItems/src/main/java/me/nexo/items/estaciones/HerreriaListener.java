package me.nexo.items.estaciones;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.Random;
import java.util.stream.Collectors;

@Singleton // 🌟 FIX: Optimizamos en memoria
public class HerreriaListener implements Listener {

    private final NexoItems plugin;
    private final Random random = new Random();

    // 🌟 FIX: Declaramos nuestro ItemManager inyectado
    private final ItemManager itemManager;

    // 🌟 FIX: Inyectamos el ItemManager en el constructor
    @Inject
    public HerreriaListener(NexoItems plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    // 🛡️ PATRÓN ENTERPRISE: InventoryHolder Personalizado (Inhackeable)
    public static class HerreriaMenuHolder implements InventoryHolder {
        private final Inventory inventory;

        // 🌟 CORRECCIÓN: Ahora acepta 'Component' en vez de 'String'
        public HerreriaMenuHolder(net.kyori.adventure.text.Component title) {
            this.inventory = Bukkit.createInventory(this, 27, title);
        }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    public void abrirMenu(Player jugador) {
        // Leemos desde la nueva arquitectura
        String rawTitle = plugin.getConfigManager().getMessages().menus().herreria().titulo();

        // 🌟 CORRECCIÓN: Guardamos el resultado como 'Component'
        net.kyori.adventure.text.Component tituloFormat = CrossplayUtils.parseCrossplay(jugador, rawTitle);

        HerreriaMenuHolder holder = new HerreriaMenuHolder(tituloFormat);
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

        inv.setItem(11, new ItemStack(Material.AIR)); // Hueco Arma
        inv.setItem(15, new ItemStack(Material.AIR)); // Hueco Material

        ItemStack yunque = new ItemStack(Material.ANVIL);
        ItemMeta metaYunque = yunque.getItemMeta();
        if (metaYunque != null) {
            String btnTitle = plugin.getConfigManager().getMessages().menus().herreria().boton().titulo();
            List<String> btnLore = plugin.getConfigManager().getMessages().menus().herreria().boton().lore();

            metaYunque.displayName(CrossplayUtils.parseCrossplay(jugador, btnTitle));
            metaYunque.lore(btnLore.stream().map(line -> CrossplayUtils.parseCrossplay(jugador, line)).collect(Collectors.toList()));
            yunque.setItemMeta(metaYunque);
        }
        inv.setItem(13, yunque);

        jugador.openInventory(inv);
    }

    @EventHandler
    public void alHacerClic(InventoryClickEvent event) {
        // 🛡️ Verificación Inhackeable
        if (!(event.getInventory().getHolder() instanceof HerreriaMenuHolder)) return;

        if (event.getClickedInventory() == null) return;
        Player jugador = (Player) event.getWhoClicked();

        // 🛡️ PROTECCIÓN ANTI-DUPE (Evita mover items raros con Shift)
        if (event.isShiftClick() && event.getClickedInventory().equals(jugador.getInventory())) {
            event.setCancelled(true);
            jugador.sendMessage("§c⚠️ Utiliza el clic normal para colocar los artefactos.");
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
                Inventory inv = event.getInventory();
                ItemStack arma = inv.getItem(11);
                ItemStack material = inv.getItem(15);

                var nodos = plugin.getConfigManager().getMessages().estaciones();

                if (arma == null || arma.getType() == Material.AIR) {
                    CrossplayUtils.sendMessage(jugador, nodos.insertaActivo());
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                // 🌟 FIX: Cambiado ItemManager estático a itemManager instanciado
                if (material == null || !material.hasItemMeta() ||
                        !material.getItemMeta().getPersistentDataContainer().has(itemManager.llaveMaterialMejora, PersistentDataType.BYTE)) {
                    CrossplayUtils.sendMessage(jugador, nodos.necesitasPolvo());
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                ItemMeta metaArma = arma.getItemMeta();
                // 🌟 FIX: Cambiado ItemManager estático a itemManager instanciado
                if (!metaArma.getPersistentDataContainer().has(itemManager.llaveNivelMejora, PersistentDataType.INTEGER)) {
                    CrossplayUtils.sendMessage(jugador, nodos.noSoportaMejoras());
                    return;
                }

                // 🌟 FIX: Cambiado ItemManager estático a itemManager instanciado
                int nivelActual = metaArma.getPersistentDataContainer().getOrDefault(itemManager.llaveNivelMejora, PersistentDataType.INTEGER, 0);

                if (nivelActual >= 10) {
                    CrossplayUtils.sendMessage(jugador, nodos.mejoraMaxima());
                    return;
                }

                // Consumimos el material
                material.setAmount(material.getAmount() - 1);

                int chanceExito = 100 - (nivelActual * 10);
                int tiro = random.nextInt(100) + 1;

                if (tiro <= chanceExito) {
                    nivelActual++;
                    // 🌟 FIX: Cambiado ItemManager estático a itemManager instanciado
                    metaArma.getPersistentDataContainer().set(itemManager.llaveNivelMejora, PersistentDataType.INTEGER, nivelActual);

                    String nombreViejo = PlainTextComponentSerializer.plainText().serialize(metaArma.displayName());
                    String nombreNuevo = nombreViejo.replaceAll("\\[\\+\\d+\\]", "[+" + nivelActual + "]");
                    metaArma.displayName(CrossplayUtils.parseCrossplay(jugador, nombreNuevo));

                    arma.setItemMeta(metaArma);

                    CrossplayUtils.sendMessage(jugador, nodos.mejoraExitosa().replace("%level%", String.valueOf(nivelActual)));
                    jugador.playSound(jugador.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                } else {
                    CrossplayUtils.sendMessage(jugador, nodos.mejoraFallida());
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        }
    }

    @EventHandler
    public void alCerrar(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof HerreriaMenuHolder)) return;

        Player jugador = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        ItemStack arma = inv.getItem(11);
        ItemStack material = inv.getItem(15);

        // 🛡️ PROTECCIÓN ANTI-VOID (Si el inventario está lleno, lo tira al suelo)
        HashMap<Integer, ItemStack> sobrantes = new HashMap<>();

        if (arma != null && arma.getType() != Material.AIR) {
            sobrantes.putAll(jugador.getInventory().addItem(arma));
        }
        if (material != null && material.getType() != Material.AIR) {
            sobrantes.putAll(jugador.getInventory().addItem(material));
        }

        // Escupimos los ítems que no cupieron al suelo real
        for (ItemStack drop : sobrantes.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), drop);
        }
    }
}
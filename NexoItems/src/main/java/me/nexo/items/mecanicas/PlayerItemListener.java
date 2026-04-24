package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.ItemManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 🎒 NexoItems - Control de Ítems Especiales y Bienvenida (Arquitectura Enterprise Java 21)
 * Rendimiento: Inyección de Dependencias, Cero Estáticos y O(1) PDC Reads.
 */
@Singleton
public class PlayerItemListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final ItemManager itemManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public PlayerItemListener(NexoItems plugin, ItemManager itemManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void alEntrar(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();

        // Si es la primera vez que entra al servidor
        if (!jugador.hasPlayedBefore()) {

            // 🌟 FIX: Uso de la instancia inyectada 'itemManager' en vez de llamada estática
            ItemStack armaInicio = itemManager.generarArmaRPG("baculo_manantial_t1");

            // Verificamos que el arma exista para no causar errores
            if (armaInicio != null) {
                jugador.getInventory().addItem(armaInicio);
            }

            // 🌟 FIX: Uso de la instancia inyectada 'crossplayUtils'
            crossplayUtils.sendMessage(jugador, "&#00f5ff✨ <bold>EL NEXO TE DA LA BIENVENIDA</bold> | Has recibido tu primer artefacto.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alTirarObjeto(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        // 🛡️ Evita que tiren ítems con la etiqueta "Soulbound" (Ligados al alma)
        // 🌟 FIX: Acceso a la llave a través de la instancia inyectada 'itemManager'
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(itemManager.llaveSoulbound, PersistentDataType.BYTE)) {
            event.setCancelled(true);

            // 🌟 FIX: Uso de la instancia inyectada 'crossplayUtils'
            crossplayUtils.sendMessage(event.getPlayer(), "&#FF5555[!] Este artefacto está ligado a tu alma. No puedes arrojarlo.");
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
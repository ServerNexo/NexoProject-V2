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
 * 🎒 NexoItems - Control de Ítems Especiales y Bienvenida (Arquitectura Enterprise)
 */
@Singleton
public class PlayerItemListener implements Listener {

    private final NexoItems plugin;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public PlayerItemListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void alEntrar(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();

        // Si es la primera vez que entra al servidor
        if (!jugador.hasPlayedBefore()) {
            ItemStack armaInicio = ItemManager.generarArmaRPG("baculo_manantial_t1");

            // Verificamos que el arma exista para no causar errores
            if (armaInicio != null) {
                jugador.getInventory().addItem(armaInicio);
            }

            // 🌟 FIX: Mensaje Directo (Adiós error de getMessage)
            CrossplayUtils.sendMessage(jugador, "&#00f5ff✨ <bold>EL NEXO TE DA LA BIENVENIDA</bold> | Has recibido tu primer artefacto.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void alTirarObjeto(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        // 🛡️ Evita que tiren ítems con la etiqueta "Soulbound" (Ligados al alma)
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ItemManager.llaveSoulbound, PersistentDataType.BYTE)) {
            event.setCancelled(true);

            // 🌟 FIX: Mensaje Directo
            CrossplayUtils.sendMessage(event.getPlayer(), "&#FF5555[!] Este artefacto está ligado a tu alma. No puedes arrojarlo.");
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
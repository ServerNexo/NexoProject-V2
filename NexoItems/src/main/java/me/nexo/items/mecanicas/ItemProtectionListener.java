package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 🎒 NexoItems - Protector de Ítems en el suelo (Arquitectura Enterprise)
 */
@Singleton
public class ItemProtectionListener implements Listener {

    private final NexoItems plugin;
    private final NamespacedKey ownerKey;
    private final NamespacedKey expireKey;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ItemProtectionListener(NexoItems plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "item_owner");
        this.expireKey = new NamespacedKey(plugin, "item_expire");
    }

    // 📦 1. Cuando un jugador tira un ítem de su inventario
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        protegerItem(event.getItemDrop(), event.getPlayer());
    }

    // ⛏️ 2. Cuando un jugador rompe un bloque y suelta ítems
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        for (Item item : event.getItems()) {
            protegerItem(item, event.getPlayer());
        }
    }

    // ⚔️ 3. Cuando un jugador mata a un mob y suelta loot
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Ignoramos si muere un jugador (su loot suele ser público o gestionado por otro plugin)
        if (event.getEntity() instanceof Player) return;

        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            // El EntityDeathEvent no nos da la entidad 'Item', nos da 'ItemStacks'.
            // Así que los spawneamos nosotros mismos y los protegemos, limpiando los originales.
            for (ItemStack drop : event.getDrops()) {
                Item itemEntity = event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), drop);
                protegerItem(itemEntity, killer);
            }
            event.getDrops().clear(); // Borramos los drops nativos para no duplicar
        }
    }

    // 🛡️ 4. LA MAGIA: Bloquear que otros lo recojan (Anti-Ninja Looting)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Item item = event.getItem();

        // Verificamos si el ítem tiene dueño
        if (item.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
            String ownerId = item.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

            // Verificamos si la protección ya expiró
            if (item.getPersistentDataContainer().has(expireKey, PersistentDataType.LONG)) {
                long expireTime = item.getPersistentDataContainer().get(expireKey, PersistentDataType.LONG);
                if (System.currentTimeMillis() > expireTime) {
                    return; // 🔓 El tiempo expiró. El ítem es público, permitimos recogerlo.
                }
            }

            // Si no expiró, y el jugador que lo intenta recoger NO es el dueño
            if (!player.getUniqueId().toString().equals(ownerId)) {
                event.setCancelled(true); // 🛑 Bloqueamos la acción silenciosamente
            }
        }
    }

    // 🔧 Helper: Función que inyecta el dueño y el tiempo a la ENTIDAD ítem en el mundo
    private void protegerItem(Item item, Player owner) {
        item.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        // Protegemos el ítem por 30 segundos (30,000 milisegundos)
        item.getPersistentDataContainer().set(expireKey, PersistentDataType.LONG, System.currentTimeMillis() + 30000L);
    }
}
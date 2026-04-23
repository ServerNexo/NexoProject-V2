package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
 * 🎒 NexoItems - Protector de Ítems en el suelo (Arquitectura Enterprise Java 21)
 * Rendimiento: Cero Dead Dependencies, Constantes O(1) y Folia Region-Safe.
 */
@Singleton
public class ItemProtectionListener implements Listener {

    // 🌟 OPTIMIZACIÓN PDC: Llaves constantes en memoria RAM (Evitan el uso de 'plugin' y recolección de basura)
    private static final NamespacedKey OWNER_KEY = new NamespacedKey("nexoitems", "item_owner");
    private static final NamespacedKey EXPIRE_KEY = new NamespacedKey("nexoitems", "item_expire");

    // 💉 PILAR 1: Inyección Estricta (Constructor vacío al no requerir dependencias externas)
    @Inject
    public ItemProtectionListener() {
    }

    // 📦 1. Cuando un jugador tira un ítem de su inventario
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        protegerItem(event.getItemDrop(), event.getPlayer());
    }

    // ⛏️ 2. Cuando un jugador rompe un bloque y suelta ítems
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        for (var item : event.getItems()) {
            protegerItem(item, event.getPlayer());
        }
    }

    // ⚔️ 3. Cuando un jugador mata a un mob y suelta loot
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Ignoramos si muere un jugador (su loot suele ser público o gestionado por otro plugin)
        if (event.getEntity() instanceof Player) return;

        var killer = event.getEntity().getKiller();
        if (killer != null) {
            var world = event.getEntity().getWorld();
            var loc = event.getEntity().getLocation();
            
            // El EntityDeathEvent no nos da la entidad 'Item', nos da 'ItemStacks'.
            // Así que los spawneamos nosotros mismos y los protegemos, limpiando los originales.
            for (var drop : event.getDrops()) {
                var itemEntity = world.dropItemNaturally(loc, drop);
                protegerItem(itemEntity, killer);
            }
            event.getDrops().clear(); // Borramos los drops nativos para no duplicar
        }
    }

    // 🛡️ 4. LA MAGIA: Bloquear que otros lo recojan (Anti-Ninja Looting)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        // Pattern Matching Java 21
        if (!(event.getEntity() instanceof Player player)) return;

        var item = event.getItem();
        var pdc = item.getPersistentDataContainer();

        // Verificamos si el ítem tiene dueño
        if (pdc.has(OWNER_KEY, PersistentDataType.STRING)) {
            String ownerId = pdc.get(OWNER_KEY, PersistentDataType.STRING);

            // Verificamos si la protección ya expiró
            if (pdc.has(EXPIRE_KEY, PersistentDataType.LONG)) {
                long expireTime = pdc.get(EXPIRE_KEY, PersistentDataType.LONG);
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
        var pdc = item.getPersistentDataContainer();
        pdc.set(OWNER_KEY, PersistentDataType.STRING, owner.getUniqueId().toString());
        // Protegemos el ítem por 30 segundos (30,000 milisegundos)
        pdc.set(EXPIRE_KEY, PersistentDataType.LONG, System.currentTimeMillis() + 30000L);
    }
}
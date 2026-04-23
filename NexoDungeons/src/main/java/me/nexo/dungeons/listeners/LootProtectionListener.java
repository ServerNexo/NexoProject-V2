package me.nexo.dungeons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 🏰 NexoDungeons - Listener de Protección de Botín (Arquitectura Enterprise)
 * Rendimiento: Cero Estáticos, Kyori Nativos y PDC Lock-Free.
 */
@Singleton
public class LootProtectionListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS (Cero llamadas estáticas a utilidades)
    private final CrossplayUtils crossplayUtils;
    private final NamespacedKey ownerKey;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public LootProtectionListener(CrossplayUtils crossplayUtils) {
        this.crossplayUtils = crossplayUtils;
        
        // 🌟 PAPER 1.21 FIX: Instanciación directa por String para no depender de la Main Class
        this.ownerKey = new NamespacedKey("nexodungeons", "loot_owner");
    }

    // =========================================
    // 🎁 UTILIDAD: Soltar Botín Estilo Hypixel
    // =========================================
    public void dropProtectedItem(Location loc, ItemStack itemStack, Player owner) {
        // 🌟 PAPER 1.21 FIX: isEmpty() previene procesar stacks fantasmas o nulos
        if (itemStack == null || itemStack.isEmpty()) return;

        // Soltamos el ítem en el mundo
        var itemEntity = loc.getWorld().dropItemNaturally(loc, itemStack);

        // Lo hacemos brillar para que se vea épico
        itemEntity.setGlowing(true);

        // 🌟 FIX KYORI ADVENTURE: Uso de componentes nativos en lugar del viejo Serializer Legacy
        String rarezaColor = "&#ff00ff<bold>"; 
        itemEntity.customName(crossplayUtils.parseCrossplay(owner, rarezaColor + "Botín de " + owner.getName() + "</bold>"));
        itemEntity.setCustomNameVisible(true);

        // 🔒 Le inyectamos el UUID del dueño en sus datos persistentes
        itemEntity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
    }

    // =========================================
    // 🛡️ LISTENER: Bloquear a los ladrones
    // =========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        // Solo nos importan los jugadores
        if (!(event.getEntity() instanceof Player player)) return;

        var item = event.getItem();
        var pdc = item.getPersistentDataContainer();

        // Verificamos si este ítem está protegido por nuestro sistema
        if (pdc.has(ownerKey, PersistentDataType.STRING)) {
            String ownerUUID = pdc.get(ownerKey, PersistentDataType.STRING);

            // Si el UUID del jugador NO coincide con el UUID guardado en el ítem...
            if (!player.getUniqueId().toString().equals(ownerUUID)) {
                event.setCancelled(true); // Bloqueamos la acción

                // 🌟 FIX: Envío inyectado en lugar del uso estático de utilidades
                crossplayUtils.sendActionBar(player, "&#FF5555[!] Ese botín le pertenece a otro jugador.");
            }
        }
    }
}
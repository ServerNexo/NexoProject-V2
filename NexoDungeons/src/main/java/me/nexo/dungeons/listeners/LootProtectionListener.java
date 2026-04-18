package me.nexo.dungeons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.utils.NexoColor;
import me.nexo.dungeons.NexoDungeons;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 🏰 NexoDungeons - Listener de Protección de Botín (Arquitectura Enterprise)
 */
@Singleton
public class LootProtectionListener implements Listener {

    private final NexoDungeons plugin;
    private final NamespacedKey ownerKey; // 🌟 FIX: Adiós al 'static'

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public LootProtectionListener(NexoDungeons plugin) {
        this.plugin = plugin;
        // Creamos la llave PDC vinculada a la instancia
        this.ownerKey = new NamespacedKey(plugin, "loot_owner");
    }

    // =========================================
    // 🎁 UTILIDAD: Soltar Botín Estilo Hypixel
    // =========================================
    // 🌟 FIX: Ahora es un método de instancia. Las clases que necesiten soltar botín
    // (como BossFightManager o LootDistributor) deben inyectar este Listener y llamarlo.
    public void dropProtectedItem(Location loc, ItemStack itemStack, Player owner) {
        if (itemStack == null || itemStack.getType().isAir()) return;

        // Soltamos el ítem en el mundo
        Item itemEntity = loc.getWorld().dropItemNaturally(loc, itemStack);

        // Lo hacemos brillar para que se vea épico
        itemEntity.setGlowing(true);

        // 🌟 FIX: Holograma flotante con colores Hexadecimales seguros para Bedrock
        String rarezaColor = "&#ff00ff<bold>"; // Puedes dinamizar esto luego si quieres
        String customName = LegacyComponentSerializer.legacySection().serialize(NexoColor.parse(rarezaColor + "Botín de " + owner.getName() + "</bold>"));
        itemEntity.setCustomName(customName);
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

        Item item = event.getItem();

        // Verificamos si este ítem está protegido por nuestro sistema
        if (item.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
            String ownerUUID = item.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

            // Si el UUID del jugador NO coincide con el UUID guardado en el ítem...
            if (!player.getUniqueId().toString().equals(ownerUUID)) {
                event.setCancelled(true); // Bloqueamos la acción

                // 🌟 FIX: Opcional, enviarle un Action Bar en lugar de un mensaje en el chat para no spamearlo.
                player.sendActionBar(NexoColor.parse("&#FF5555[!] Ese botín le pertenece a otro jugador."));
            }
        }
    }
}
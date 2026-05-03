package me.nexo.dungeons.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.dungeons.mechanics.AbyssBackpackManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 🧲 NexoDungeons - Interceptor de Loot del Abismo
 * Evita que los ítems entren al inventario real y los redirige a la mochila virtual.
 */
@Singleton
public class AbyssLootListener implements Listener {

    private final AbyssBackpackManager backpackManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 FIX: Hemos eliminado DungeonSlimeManager de la inyección para hacer la clase más ligera
    @Inject
    public AbyssLootListener(AbyssBackpackManager backpackManager, CrossplayUtils crossplayUtils) {
        this.backpackManager = backpackManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAbyssLootPickup(EntityPickupItemEvent event) {
        // 1. Filtro: ¿Es un jugador?
        if (!(event.getEntity() instanceof Player player)) return;

        // 🌟 FIX: Filtro O(1) nativo verificando el prefijo del mundo (Igual que en AbyssDeathListener)
        String worldName = player.getWorld().getName().toLowerCase();
        if (!worldName.startsWith("dungeon_") && !worldName.startsWith("inst_")) return;

        // 2. Cancelamos la mecánica Vanilla (El ítem NO entra a su inventario real)
        event.setCancelled(true);

        ItemStack item = event.getItem().getItemStack();

        // 3. Lo inyectamos a nuestra RAM mediante hilos virtuales
        backpackManager.addItemAsync(player.getUniqueId(), item);

        // 4. Eliminamos la entidad física del suelo
        event.getItem().remove();

        // =========================================================
        // 🌟 INMERSIÓN AAA: Feedback visual y sonoro (Action Bar)
        // =========================================================
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.5f);

        String itemName = getDisplayName(item);
        // Formato visual: "+ 5x Diamante (Mochila del Abismo)"
        crossplayUtils.sendActionBar(player, "&#55FF55+ " + item.getAmount() + "x " + itemName + " &#E6CCFF(Mochila)");
    }

    /**
     * Utilidad para obtener el nombre real del ítem (Soporte para NexoItems/Oraxen).
     */
    private String getDisplayName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                // Si usa Kyori MiniMessage, extraemos el texto plano o renderizamos los colores
                return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            }
        }
        // Traduce MATERIAL_NAME a Material Name
        String name = item.getType().name().replace("_", " ").toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
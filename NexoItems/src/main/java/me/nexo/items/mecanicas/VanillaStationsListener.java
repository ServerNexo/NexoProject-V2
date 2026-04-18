package me.nexo.items.mecanicas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 🎒 NexoItems - Bloqueador de Estaciones Vanilla (Arquitectura Enterprise)
 */
@Singleton
public class VanillaStationsListener implements Listener {

    private final NexoItems plugin;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public VanillaStationsListener(NexoItems plugin) {
        this.plugin = plugin;
    }

    // 🔨 1. Bloquear Yunques Vanilla
    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack slot1 = event.getInventory().getItem(0);
        ItemStack slot2 = event.getInventory().getItem(1);

        if (esItemCustom(slot1) || esItemCustom(slot2)) {
            event.setResult(null); // Borra el ítem de resultado visualmente
        }
    }

    // 🪨 2. Bloquear Piedras de Afilar (Grindstones)
    @EventHandler
    public void onGrindstone(PrepareGrindstoneEvent event) {
        ItemStack slot1 = event.getInventory().getItem(0);
        ItemStack slot2 = event.getInventory().getItem(1);

        if (esItemCustom(slot1) || esItemCustom(slot2)) {
            event.setResult(null);
        }
    }

    // ✨ 3. Bloquear Mesas de Encantamiento
    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent event) {
        if (esItemCustom(event.getItem())) {
            event.setCancelled(true); // Cancela el encantamiento
        }
    }

    // 🛡️ 4. Bloquear Mesa de Herrería (Smithing Table 1.20+)
    @EventHandler
    public void onSmithing(PrepareSmithingEvent event) {
        // En 1.20+, hay 3 slots de entrada (Template, Base, Material)
        for (int i = 0; i < 3; i++) {
            ItemStack inputSlot = event.getInventory().getItem(i);
            if (esItemCustom(inputSlot)) {
                event.setResult(null); // Si tan solo UNO es de Nexo, bloqueamos la mesa
                return;
            }
        }
    }

    // 🧠 MAGIA SENIOR: Detecta cualquier ítem de tu ecosistema sin importar qué sea
    private boolean esItemCustom(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        // Escaneamos todas las llaves (keys) de datos ocultos del ítem
        for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
            // Si la llave del NBT pertenece a tu ecosistema, lo identificamos al instante
            if (key.getNamespace().toLowerCase().contains("nexo")) {
                return true;
            }
        }
        return false;
    }
}
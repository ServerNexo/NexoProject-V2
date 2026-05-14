package me.nexo.mechanics.lategame.fracture;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ☄️ Rastreador de Momentum Orbital (NexoFracture)
 * Premia la minería acrobática en gravedad cero (sin tocar el suelo).
 */
@Singleton
public class MomentumTracker implements Listener {

    private final Map<UUID, Integer> airborneCombo = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public MomentumTracker(NexoMechanics plugin) {
    }

    @SuppressWarnings("deprecation") // 🌟 FIX: Silenciamos el aviso. Priorizamos el rendimiento de lectura del boolean.
    @EventHandler
    public void onAsteroidMine(BlockBreakEvent event) {
        if (!event.getBlock().getWorld().getName().equals("nexo_fracture")) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // ==========================================
        // 🌟 VERIFICAR HERRAMIENTA LATE-GAME
        // ==========================================
        org.bukkit.inventory.ItemStack handItem = player.getInventory().getItemInMainHand();

        // Aquí verificamos si el ítem es tu herramienta especial (ajustar el nombre o NBT según tu config de Nexo/Oraxen)
        boolean isUsingGrapple = handItem != null && handItem.hasItemMeta()
                && handItem.getItemMeta().hasDisplayName()
                && handItem.getItemMeta().getDisplayName().contains("Anchor Grapple");

        // Si usa un pico normal, simplemente no hace nada (no suma combo)
        if (!isUsingGrapple) return;

        // ==========================================
        // 🌟 LÓGICA DE MOMENTUM ACROBÁTICO
        // ==========================================
        if (!player.isOnGround()) {
            int combo = airborneCombo.getOrDefault(uuid, 0) + 1;
            airborneCombo.put(uuid, combo);

            // Feedback visual épico
            if (combo % 5 == 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f + (combo * 0.05f));
                player.sendActionBar(mm.deserialize("<aqua>☄️ ¡Momentum x" + combo + "! Multiplicador de Botín Activado.</aqua>"));

                // 💡 Aquí conectarías con tu LootDistributor para darle x2 o x3 en Ores
            }
        } else {
            // Tocó el suelo o el asteroide. Pierde el combo.
            if (airborneCombo.containsKey(uuid) && airborneCombo.get(uuid) >= 5) {
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
                player.sendActionBar(mm.deserialize("<red>💥 Momentum perdido por impacto.</red>"));
            }
            airborneCombo.remove(uuid);
        }
    }
}
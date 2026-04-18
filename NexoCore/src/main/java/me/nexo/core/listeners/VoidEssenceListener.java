package me.nexo.core.listeners;

import me.nexo.core.NexoCore;
import me.nexo.core.user.NexoUser;
import me.nexo.core.utils.NexoColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

public class VoidEssenceListener implements Listener {

    private final NexoCore core;

    public VoidEssenceListener(NexoCore core) {
        this.core = core;
    }

    @EventHandler
    public void onConsumeEssence(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        // 🔮 Validación rápida del ítem (Podemos mejorarlo con NBT Tags/PersistentDataContainer)
        if (item.getType() == Material.AMETHYST_SHARD && item.getItemMeta().getDisplayName().contains("Esencia del Vacío")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            NexoUser user = core.getUserManager().getUserOrNull(player.getUniqueId());
            if (user == null) return;

            // Consumir 1 ítem
            item.setAmount(item.getAmount() - 1);

            // Sumar 24 Horas (86,400,000 milisegundos)
            user.addVoidBlessingTime(86400000L);

            // 🌟 VIVID VOID: Notificación Inmersiva
            player.sendMessage(NexoColor.parse("&#ff00ff======================================"));
            player.sendMessage(NexoColor.parse("&#ff00ff[✧] El poder del Vacío corre por tus venas."));
            player.sendMessage(NexoColor.parse("&#00f5ff+24 Horas de Bendición del Vacío añadidas."));
            player.sendMessage(NexoColor.parse("&#ff00ff======================================"));

            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

            // Guardado Asíncrono para seguridad (Zero-Main-Thread SQL)
            CompletableFuture.runAsync(() -> {
                String sql = "UPDATE jugadores SET void_blessing_until = ? WHERE uuid = ?";
                try (java.sql.Connection conn = core.getDatabaseManager().getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, user.getVoidBlessingUntil());
                    ps.setString(2, user.getUuid().toString());
                    ps.executeUpdate();
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }
}
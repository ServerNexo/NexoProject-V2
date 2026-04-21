package me.nexo.core.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.database.DatabaseManager;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.core.utils.NexoColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏛️ Nexo Network - Listener de Esencias (Arquitectura Enterprise)
 * Optimizado para Java 21+ con I/O en Hilos Virtuales y Kyori Adventure API.
 */
@Singleton
public class VoidEssenceListener implements Listener {

    private final UserManager userManager;
    private final DatabaseManager databaseManager;
    private final NexoColor nexoColor;
    
    // 🚀 Motor de concurrencia Zero-Lag para la Base de Datos
    private final ExecutorService virtualExecutor;

    // 💉 PILAR 1: Inyección estricta, sin acoplamiento a "NexoCore"
    @Inject
    public VoidEssenceListener(UserManager userManager, DatabaseManager databaseManager, NexoColor nexoColor) {
        this.userManager = userManager;
        this.databaseManager = databaseManager;
        this.nexoColor = nexoColor;
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @EventHandler
    public void onConsumeEssence(PlayerInteractEvent event) {
        // 🌟 Paper 1.21.5: Método moderno para verificar clics
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return;

        // 🔮 Validación moderna Paper 1.21.5 (Lectura plana del Componente de Kyori)
        if (item.getType() == Material.AMETHYST_SHARD && item.getItemMeta().hasDisplayName()) {
            String plainName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
            
            if (plainName.contains("Esencia del Vacío")) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                NexoUser user = userManager.getUserOrNull(player.getUniqueId());
                if (user == null) return;

                // 🌟 Paper 1.21.5: Método seguro para restar ítems
                item.subtract(1);

                // Sumar 24 Horas (86,400,000 milisegundos)
                user.addVoidBlessingTime(86400000L);

                // 🌟 VIVID VOID: Notificación Inmersiva con inyección de NexoColor
                player.sendMessage(nexoColor.parse("&#ff00ff======================================"));
                player.sendMessage(nexoColor.parse("&#ff00ff[✧] El poder del Vacío corre por tus venas."));
                player.sendMessage(nexoColor.parse("&#00f5ff+24 Horas de Bendición del Vacío añadidas."));
                player.sendMessage(nexoColor.parse("&#ff00ff======================================"));

                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

                // 🚀 Guardado Asíncrono en Hilo Virtual (Zero-Main-Thread SQL)
                CompletableFuture.runAsync(() -> {
                    String sql = "UPDATE jugadores SET void_blessing_until = ? WHERE uuid = ?";
                    // 🌟 Uso de 'var' (Java 10+)
                    try (var conn = databaseManager.getConnection();
                         var ps = conn.prepareStatement(sql)) {
                        ps.setLong(1, user.getVoidBlessingUntil());
                        ps.setString(2, user.getUuid().toString());
                        ps.executeUpdate();
                    } catch (Exception e) { 
                        e.printStackTrace(); 
                    }
                }, virtualExecutor);
            }
        }
    }
}
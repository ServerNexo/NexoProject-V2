package me.nexo.economy.bazar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 💰 NexoEconomy - Listener de Chat para Órdenes del Bazar (Arquitectura Enterprise)
 */
@Singleton
public class BazaarChatListener implements Listener {

    private final NexoEconomy plugin;
    private final BazaarManager bazaarManager;

    public static final Map<UUID, OrderSession> activeSessions = new HashMap<>();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public BazaarChatListener(NexoEconomy plugin, BazaarManager bazaarManager) {
        this.plugin = plugin;
        this.bazaarManager = bazaarManager;
    }

    public static class OrderSession {
        public String itemId;
        public String orderType;
        public int amount = -1;

        public OrderSession(String itemId, String orderType) {
            this.itemId = itemId;
            this.orderType = orderType;
        }
    }

    // Usamos LOWEST para cancelar el chat antes de que otros plugins (como chat formattings) lo procesen
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        OrderSession session = activeSessions.get(player.getUniqueId());
        String msg = event.getMessage().trim().toLowerCase();

        if (msg.equals("cancelar") || msg.equals("cancel")) {
            activeSessions.remove(player.getUniqueId());
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Operación abortada. Has cancelado la creación de la orden.");
            return;
        }

        try {
            // 🌟 FIX: Soporte inteligente para formato de una sola línea (Ej: "64 10")
            String[] parts = msg.split(" ");

            if (parts.length == 2 && session.amount == -1) {
                int cant = Integer.parseInt(parts[0]);
                BigDecimal price = new BigDecimal(parts[1]);

                if (cant <= 0 || price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                activeSessions.remove(player.getUniqueId());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (session.orderType.equals("BUY")) {
                        bazaarManager.crearOrdenCompra(player, session.itemId, cant, price);
                    } else {
                        bazaarManager.crearOrdenVenta(player, session.itemId, cant, price);
                    }
                });
                return;
            }

            // Lógica de dos pasos (por si el jugador solo escribe la cantidad primero)
            if (session.amount == -1) {
                int cant = Integer.parseInt(msg);
                if (cant <= 0) throw new NumberFormatException();

                session.amount = cant;
                CrossplayUtils.sendMessage(player, "&#00f5ff[⚖] <bold>BAZAR:</bold> &#E6CCFFCantidad fijada en: &#55FF55" + cant);
                CrossplayUtils.sendMessage(player, "&#E6CCFFAhora escribe el PRECIO UNITARIO al que deseas comerciar:");
            } else {
                BigDecimal price = new BigDecimal(msg);
                if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                activeSessions.remove(player.getUniqueId());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (session.orderType.equals("BUY")) {
                        bazaarManager.crearOrdenCompra(player, session.itemId, session.amount, price);
                    } else {
                        bazaarManager.crearOrdenVenta(player, session.itemId, session.amount, price);
                    }
                });
            }
        } catch (NumberFormatException e) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Valor inválido. Escribe un número válido (Ej: 64 10) o 'cancelar'.");
        }
    }
}
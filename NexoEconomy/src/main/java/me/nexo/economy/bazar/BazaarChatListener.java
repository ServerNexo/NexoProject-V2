package me.nexo.economy.bazar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.math.BigDecimal;

/**
 * 💰 NexoEconomy - Listener de Chat para Órdenes del Bazar (Arquitectura Enterprise Java 21)
 * Rendimiento: AsyncChatEvent Nativo, Folia Region Scheduler y Transiciones de Estado Inmutables.
 */
@Singleton
public class BazaarChatListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoEconomy plugin;
    private final BazaarManager bazaarManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa (Cero Mapas Estáticos)
    @Inject
    public BazaarChatListener(NexoEconomy plugin, BazaarManager bazaarManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.bazaarManager = bazaarManager;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 PAPER NATIVE: AsyncChatEvent reemplaza al obsoleto AsyncPlayerChatEvent
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        var session = bazaarManager.getChatSession(player.getUniqueId());
        
        if (session == null) return;

        event.setCancelled(true);
        
        // Extraemos el texto plano de forma segura utilizando la API de Kyori Adventure
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim().toLowerCase();

        if (msg.equals("cancelar") || msg.equals("cancel")) {
            bazaarManager.removeChatSession(player.getUniqueId());
            crossplayUtils.sendMessage(player, "&#FF5555[!] Operación abortada. Has cancelado la creación de la orden.");
            return;
        }

        try {
            // 🌟 FIX: Soporte inteligente para formato de una sola línea (Ej: "64 10")
            String[] parts = msg.split(" ");

            // Validamos que sea el formato de 2 partes y que aún no haya introducido la cantidad
            if (parts.length == 2 && session.amount() == -1) {
                int cant = Integer.parseInt(parts[0]);
                var price = new BigDecimal(parts[1]);

                if (cant <= 0 || price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                bazaarManager.removeChatSession(player.getUniqueId());

                // 🌟 FOLIA SYNC: Devolvemos la ejecución a la región del jugador para modificar su inventario
                player.getScheduler().run(plugin, task -> {
                    if (session.orderType().equals("BUY")) {
                        bazaarManager.crearOrdenCompra(player, session.itemId(), cant, price);
                    } else {
                        bazaarManager.crearOrdenVenta(player, session.itemId(), cant, price);
                    }
                }, null);
                return;
            }

            // Lógica de dos pasos (Si el jugador solo escribe la cantidad primero)
            if (session.amount() == -1) {
                int cant = Integer.parseInt(msg);
                if (cant <= 0) throw new NumberFormatException();

                // 🌟 INMUTABILIDAD: Reemplazamos la sesión en el mapa con la cantidad actualizada
                bazaarManager.iniciarSesionChat(player.getUniqueId(), session.itemId(), session.orderType(), cant);
                
                crossplayUtils.sendMessage(player, "&#00f5ff[⚖] <bold>BAZAR:</bold> &#E6CCFFCantidad fijada en: &#55FF55" + cant);
                crossplayUtils.sendMessage(player, "&#E6CCFFAhora escribe el PRECIO UNITARIO al que deseas comerciar:");
            } else {
                // Segundo paso: Ya tenemos la cantidad, ahora procesamos el precio
                var price = new BigDecimal(msg);
                if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();

                bazaarManager.removeChatSession(player.getUniqueId());

                player.getScheduler().run(plugin, task -> {
                    if (session.orderType().equals("BUY")) {
                        bazaarManager.crearOrdenCompra(player, session.itemId(), session.amount(), price);
                    } else {
                        bazaarManager.crearOrdenVenta(player, session.itemId(), session.amount(), price);
                    }
                }, null);
            }
        } catch (NumberFormatException e) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Valor inválido. Escribe un número válido (Ej: 64 10) o 'cancelar'.");
        }
    }
}
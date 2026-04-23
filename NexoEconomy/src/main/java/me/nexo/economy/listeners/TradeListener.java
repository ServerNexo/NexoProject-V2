package me.nexo.economy.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import me.nexo.economy.trade.TradeManager;
import me.nexo.economy.trade.TradeSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigDecimal;

/**
 * 💰 NexoEconomy - Listener de Intercambios (Arquitectura Enterprise Inhackeable)
 * Rendimiento: Dependencias Aisladas, Cero Estáticos y Pattern Matching nativo.
 */
@Singleton
public class TradeListener implements Listener {

    // 🌟 DEPENDENCIAS PROPAGADAS (Sinergia absoluta)
    private final TradeManager tradeManager;
    private final EconomyManager economyManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public TradeListener(TradeManager tradeManager, EconomyManager economyManager, CrossplayUtils crossplayUtils) {
        this.tradeManager = tradeManager;
        this.economyManager = economyManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 🛡️ PATRÓN ENTERPRISE: Validación 100% segura usando el Holder nativo
        if (!(event.getInventory().getHolder() instanceof TradeSession session)) return;

        // 🛡️ ANTI-DUPE: Cancelamos Shift-Clicks para evitar desbordes de arrays e ítems fantasma
        if (event.isShiftClick() && event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            event.setCancelled(true);
            crossplayUtils.sendMessage(player, "&#FF5555[!] Por seguridad, usa el clic normal para mover ítems al intercambio.");
            return;
        }

        if (event.getClickedInventory() == null) return;

        // Si toca su propio inventario (el de abajo), desmarcamos el "Listo" pero permitimos mover sus ítems
        if (event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            session.unready();
            return;
        }

        int slot = event.getSlot();
        boolean isPlayer1 = player.equals(session.getPlayer1());

        // Fila central (Divisor y botones de dinero)
        if (slot % 9 == 4) {
            event.setCancelled(true); // Nadie puede tocar el divisor

            // 🌟 FIX DINERO: Cobramos instantáneamente de la cuenta para evitar deudas o dupes
            if (slot == 13) procesarAumentoDinero(player, session, NexoAccount.Currency.COINS, new BigDecimal("1000"));
            else if (slot == 22) procesarAumentoDinero(player, session, NexoAccount.Currency.GEMS, new BigDecimal("100"));
            else if (slot == 31) procesarAumentoDinero(player, session, NexoAccount.Currency.MANA, new BigDecimal("10"));
            return;
        }

        boolean isLeftSide = (slot % 9 < 4);

        // Botón de Listo P1
        if (slot == 45 && isPlayer1) {
            event.setCancelled(true);
            session.toggleReady(player);
            return;
        }

        // Botón de Listo P2
        if (slot == 53 && !isPlayer1) {
            event.setCancelled(true);
            session.toggleReady(player);
            return;
        }

        // Bloquear tocar cualquier otro botón inferior (45 al 53)
        if (slot >= 45) {
            event.setCancelled(true);
            return;
        }

        // Evitar que toquen el lado del otro jugador
        if (isPlayer1 && !isLeftSide) {
            event.setCancelled(true);
        } else if (!isPlayer1 && isLeftSide) {
            event.setCancelled(true);
        } else {
            session.unready(); // Si movió un ítem válido, desmarcamos el botón de listo
        }
    }

    // 🛡️ ANTI-DUPE: Bloquear arrastre masivo de ítems (Drag Exploit)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTradeDrag(InventoryDragEvent event) {
        // 🌟 JAVA 21 NATIVO: Pattern Matching ultra-rápido
        if (event.getInventory().getHolder() instanceof TradeSession) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Por seguridad, arrastrar ítems está bloqueado. Muévelos de uno en uno.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTradeClose(InventoryCloseEvent event) {
        // 🛡️ CIERRE ATÓMICO: Si alguien cierra con ESC, se aborta y se devuelven fondos e ítems automáticamente
        if (event.getInventory().getHolder() instanceof TradeSession session) {
            session.abortTrade("Una de las partes ha cerrado la ventana de intercambio.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        var session = tradeManager.getSession(player);

        if (session != null) {
            // 🛡️ ABORTO ATÓMICO POR DESCONEXIÓN
            session.abortTrade("El jugador " + player.getName() + " se ha desconectado del servidor.");
        }
    }

    /**
     * 🌟 Lógica Segura de Dinero: Verifica fondos, los descuenta, y luego los añade a la mesa.
     * (Si el trade se cancela, session.abortTrade() los reembolsará automáticamente).
     */
    private void procesarAumentoDinero(Player player, TradeSession session, NexoAccount.Currency currency, BigDecimal amount) {
        // Usando la inyección directa en lugar de pasar por el Plugin Main
        boolean tieneFondos = economyManager.hasBalance(player.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amount);

        if (tieneFondos) {
            // 1. Deducimos el dinero de su cuenta real instantáneamente
            economyManager.updateBalanceAsync(player.getUniqueId(), NexoAccount.AccountType.PLAYER, currency, amount, false);

            // 2. Lo añadimos a la mesa de intercambio
            session.addCurrency(player, currency, amount);
            crossplayUtils.sendMessage(player, "&#55FF55[+] Añadido al intercambio.");
        } else {
            crossplayUtils.sendMessage(player, "&#FF5555[!] No tienes fondos suficientes para añadir esa cantidad.");
        }
    }
}
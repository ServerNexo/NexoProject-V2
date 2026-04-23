package me.nexo.economy.trade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.core.EconomyManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 💰 NexoEconomy - Manager de Intercambios (Arquitectura Enterprise)
 * Rendimiento: Caffeine Cache, Propagación de Dependencias y Bloqueos de Concurrencia Doble.
 */
@Singleton
public class TradeManager {

    // 🌟 DEPENDENCIAS INYECTADAS
    private final EconomyManager economyManager;
    private final CrossplayUtils crossplayUtils;

    // Caché inteligente: Las peticiones expiran automáticamente a los 60 segundos (O(1))
    private final Cache<UUID, UUID> tradeRequests;

    // ConcurrentHashMap para lecturas lock-free y sincronización estricta en escrituras.
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public TradeManager(EconomyManager economyManager, CrossplayUtils crossplayUtils) {
        this.economyManager = economyManager;
        this.crossplayUtils = crossplayUtils;
        
        this.tradeRequests = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    public void enviarPeticion(Player sender, Player target) {
        // Bloqueo rápido: No puedes enviar peticiones si ya estás en un trade
        if (activeSessions.containsKey(sender.getUniqueId()) || activeSessions.containsKey(target.getUniqueId())) {
            crossplayUtils.sendMessage(sender, "&#FF5555[!] Uno de los jugadores ya se encuentra en un intercambio.");
            return;
        }

        tradeRequests.put(target.getUniqueId(), sender.getUniqueId());

        crossplayUtils.sendMessage(sender, "&#00f5ff[🤝] <bold>TRADE:</bold> &#E6CCFFHas enviado una petición de intercambio a &#ff00ff" + target.getName() + "&#E6CCFF.");
        crossplayUtils.sendMessage(target, "&#00f5ff[🤝] <bold>TRADE:</bold> &#ff00ff" + sender.getName() + " &#E6CCFFquiere intercambiar contigo. Usa &#55FF55/trade " + sender.getName() + " &#E6CCFFpara aceptar.");
    }

    public boolean tienePeticionDe(Player target, Player sender) {
        var savedSender = tradeRequests.getIfPresent(target.getUniqueId());
        return savedSender != null && savedSender.equals(sender.getUniqueId());
    }

    // 🌟 FIX FOLIA: Usamos 'synchronized' para garantizar la inserción atómica de DOS llaves al mismo tiempo.
    // Esto evita que hilos de chunks diferentes crucen validaciones.
    public synchronized void iniciarTrade(Player player1, Player player2) {
        if (activeSessions.containsKey(player1.getUniqueId()) || activeSessions.containsKey(player2.getUniqueId())) {
            crossplayUtils.sendMessage(player1, "&#FF5555[!] Transacción abortada. Uno de los jugadores ya está en un intercambio.");
            crossplayUtils.sendMessage(player2, "&#FF5555[!] Transacción abortada. Uno de los jugadores ya está en un intercambio.");
            return;
        }

        // Limpiamos las peticiones pendientes para evitar spam
        tradeRequests.invalidate(player1.getUniqueId());
        tradeRequests.invalidate(player2.getUniqueId());

        // 🌟 INYECCIÓN TRANSITIVA: Pasamos las instancias a la sesión en lugar de la clase Main
        var session = new TradeSession(economyManager, this, crossplayUtils, player1, player2);
        
        activeSessions.put(player1.getUniqueId(), session);
        activeSessions.put(player2.getUniqueId(), session);

        session.open();
    }

    public TradeSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    // 🌟 Sincronizamos la remoción también para mantener integridad estricta
    public synchronized void removeSession(TradeSession session) {
        if (session == null) return;
        activeSessions.remove(session.getPlayer1().getUniqueId());
        activeSessions.remove(session.getPlayer2().getUniqueId());
    }
}
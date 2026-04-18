package me.nexo.economy.trade;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 💰 NexoEconomy - Manager de Intercambios (Arquitectura Enterprise Java 25)
 * Blindaje Anti-Dupe mediante Colecciones Concurrentes y Checkeos Atómicos.
 */
@Singleton
public class TradeManager {

    private final NexoEconomy plugin;

    // Caché inteligente: Las peticiones expiran automáticamente a los 60 segundos
    private final Cache<UUID, UUID> tradeRequests;

    // 🚨 FIX CRÍTICO ANTI-DUPE: HashMap normal crashea en concurrencia o permite dupes.
    // ConcurrentHashMap asegura transacciones atómicas a nivel de memoria RAM.
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public TradeManager(NexoEconomy plugin) {
        this.plugin = plugin;
        this.tradeRequests = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    public void enviarPeticion(Player sender, Player target) {
        // Bloqueo rápido: No puedes enviar peticiones si ya estás en un trade
        if (activeSessions.containsKey(sender.getUniqueId()) || activeSessions.containsKey(target.getUniqueId())) {
            CrossplayUtils.sendMessage(sender, "&#FF5555[!] Uno de los jugadores ya se encuentra en un intercambio.");
            return;
        }

        tradeRequests.put(target.getUniqueId(), sender.getUniqueId());

        CrossplayUtils.sendMessage(sender, "&#00f5ff[🤝] <bold>TRADE:</bold> &#E6CCFFHas enviado una petición de intercambio a &#ff00ff" + target.getName() + "&#E6CCFF.");
        CrossplayUtils.sendMessage(target, "&#00f5ff[🤝] <bold>TRADE:</bold> &#ff00ff" + sender.getName() + " &#E6CCFFquiere intercambiar contigo. Usa &#55FF55/trade " + sender.getName() + " &#E6CCFFpara aceptar.");
    }

    public boolean tienePeticionDe(Player target, Player sender) {
        UUID savedSender = tradeRequests.getIfPresent(target.getUniqueId());
        return savedSender != null && savedSender.equals(sender.getUniqueId());
    }

    public void iniciarTrade(Player player1, Player player2) {
        // 🛡️ CHECK ATÓMICO: Evita que un jugador abra dos trades a la vez con un autoclicker (Dupe Glitch)
        if (activeSessions.containsKey(player1.getUniqueId()) || activeSessions.containsKey(player2.getUniqueId())) {
            CrossplayUtils.sendMessage(player1, "&#FF5555[!] Transacción abortada. Uno de los jugadores ya está en un intercambio.");
            CrossplayUtils.sendMessage(player2, "&#FF5555[!] Transacción abortada. Uno de los jugadores ya está en un intercambio.");
            return;
        }

        // Limpiamos las peticiones pendientes para evitar spam
        tradeRequests.invalidate(player1.getUniqueId());
        tradeRequests.invalidate(player2.getUniqueId());

        // Usamos var (Java 25) para instanciar la sesión limpia
        var session = new TradeSession(plugin, player1, player2);
        activeSessions.put(player1.getUniqueId(), session);
        activeSessions.put(player2.getUniqueId(), session);

        session.open();
    }

    public TradeSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void removeSession(TradeSession session) {
        if (session == null) return;
        activeSessions.remove(session.getPlayer1().getUniqueId());
        activeSessions.remove(session.getPlayer2().getUniqueId());
    }
}
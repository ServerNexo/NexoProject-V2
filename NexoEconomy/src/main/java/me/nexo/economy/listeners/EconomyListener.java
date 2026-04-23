package me.nexo.economy.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.core.EconomyManager;
import me.nexo.economy.core.NexoAccount;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 💰 NexoEconomy - Listener de Cuentas Bancarias (Arquitectura Enterprise Java 21)
 * Rendimiento: Carga asíncrona segura y prevención de Memory Leaks de la entidad Player.
 */
@Singleton
public class EconomyListener implements Listener {

    private final NexoEconomy plugin;
    private final EconomyManager economyManager;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public EconomyListener(NexoEconomy plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager; 
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        
        // 🌟 FIX RAM: Extraemos datos inmutables antes de entrar al hilo asíncrono.
        // Retener el objeto 'Player' dentro de un CompletableFuture causa una fuga de memoria
        // si el usuario aborta la conexión milisegundos después de entrar.
        var playerId = player.getUniqueId();
        var playerName = player.getName();

        // Carga la cuenta del jugador en la RAM (o la crea si es nuevo) sin dar lag al server
        economyManager.getAccountAsync(playerId, NexoAccount.AccountType.PLAYER).thenAccept(account -> {
            if (account != null) {
                // Comentado para evitar spam en la consola si entran 100 jugadores de golpe,
                // pero puedes descomentarlo para hacer pruebas.
                // plugin.getLogger().info("Billetera cargada para: " + playerName);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("❌ Error cargando la billetera de " + playerName + ": " + ex.getMessage());
            return null;
        });
    }
}
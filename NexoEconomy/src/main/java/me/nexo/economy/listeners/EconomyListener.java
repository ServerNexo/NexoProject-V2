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
 * 💰 NexoEconomy - Listener de Cuentas Bancarias (Arquitectura Enterprise)
 */
@Singleton
public class EconomyListener implements Listener {

    private final NexoEconomy plugin;
    private final EconomyManager economyManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public EconomyListener(NexoEconomy plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager; // ¡Inyectado directo a la vena!
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Carga la cuenta del jugador en la RAM (o la crea si es nuevo) sin dar lag al server
        economyManager.getAccountAsync(event.getPlayer().getUniqueId(), NexoAccount.AccountType.PLAYER).thenAccept(account -> {
            if (account != null) {
                // Comentado para evitar spam en la consola si entran 100 jugadores de golpe,
                // pero puedes descomentarlo para hacer pruebas.
                // plugin.getLogger().info("Billetera cargada para: " + event.getPlayer().getName());
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("❌ Error cargando la billetera de " + event.getPlayer().getName() + ": " + ex.getMessage());
            return null;
        });
    }
}
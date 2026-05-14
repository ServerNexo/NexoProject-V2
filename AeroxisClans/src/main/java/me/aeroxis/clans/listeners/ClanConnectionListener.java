package me.aeroxis.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.clans.AeroxisClans;
import me.aeroxis.clans.core.ClanManager;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.user.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 👥 AeroxisClans - Escucha de Conexiones (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales asíncronos gestionados por Executor (Zero-Lag).
 */
@Singleton
public class ClanConnectionListener implements Listener {

    private final AeroxisClans plugin;
    private final ClanManager clanManager;
    private final UserManager userManager;

    // 🌟 FIX: Instanciación formal del Executor de Hilos Virtuales para tareas I/O
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 💉 PILAR 1: Inyección de Dependencias Directa (Cero acoplamiento)
    @Inject
    public ClanConnectionListener(AeroxisClans plugin, ClanManager clanManager, UserManager userManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.userManager = userManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 🌟 FIX: Uso del Executor formal de Hilos Virtuales
        virtualExecutor.submit(() -> {
            try {
                // Hacemos una pausa asíncrona de 1 segundo (1000ms) sin bloquear absolutamente nada.
                // Le damos tiempo a AeroxisCore de descargar el perfil de Supabase.
                Thread.sleep(1000);

                // Pedimos el usuario directamente a la memoria RAM inyectada
                AeroxisUser user = userManager.getUserOrNull(player.getUniqueId());

                // Si el usuario existe y pertenece a un clan, lo metemos a la caché Caffeine de Clanes
                if (user != null && user.hasClan()) {
                    clanManager.loadClanAsync(user.getClanId(), clan -> {
                        // Operación silenciosa. El clan ya está listo en O(1) para cuando abra el menú.
                    });
                }

            } catch (InterruptedException e) {
                plugin.getLogger().warning("⚠️ La carga del clan para " + player.getName() + " fue interrumpida.");
                // 🌟 FIX CRÍTICO: Restaurar la bandera de interrupción del hilo
                Thread.currentThread().interrupt(); 
            }
        });
    }
}
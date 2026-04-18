package me.nexo.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.NexoClans;
import me.nexo.clans.core.ClanManager;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 👥 NexoClans - Escucha de Conexiones (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales asíncronos en lugar de retrasos en el Tick Loop de Bukkit.
 */
@Singleton
public class ClanConnectionListener implements Listener {

    private final NexoClans plugin;
    private final ClanManager clanManager;
    private final UserManager userManager;

    // 💉 PILAR 3: Inyección de Dependencias Directa (Cero acoplamiento)
    @Inject
    public ClanConnectionListener(NexoClans plugin, ClanManager clanManager, UserManager userManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.userManager = userManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 🌟 FIX: Hilo Virtual. No ensucia el hilo principal del servidor.
        Thread.startVirtualThread(() -> {
            try {
                // Hacemos una pausa asíncrona de 1 segundo (1000ms) sin bloquear absolutamente nada.
                // Le damos tiempo a NexoCore de descargar el perfil de Supabase.
                Thread.sleep(1000);

                // Pedimos el usuario directamente a la memoria RAM inyectada
                NexoUser user = userManager.getUserOrNull(player.getUniqueId());

                // Si el usuario existe y pertenece a un clan, lo metemos a la caché Caffeine de Clanes
                if (user != null && user.hasClan()) {
                    clanManager.loadClanAsync(user.getClanId(), clan -> {
                        // Operación silenciosa. El clan ya está listo en O(1) para cuando abra el menú.
                    });
                }

            } catch (InterruptedException e) {
                plugin.getLogger().warning("⚠️ La carga del clan para " + player.getName() + " fue interrumpida.");
            }
        });
    }
}
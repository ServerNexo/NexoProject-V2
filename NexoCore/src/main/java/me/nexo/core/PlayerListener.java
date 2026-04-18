package me.nexo.core; // Si decides moverlo a la carpeta listeners, cambia esto a me.nexo.core.listeners

import com.google.inject.Inject;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 🏛️ Nexo Network - Gestor de Conexiones (Arquitectura Enterprise)
 * Carga y guardado de jugadores 100% asíncrono.
 */
public class PlayerListener implements Listener {

    // 💉 PILAR 3: Solo inyectamos el Repositorio de BD y el Gestor de Caché
    private final UserRepository userRepository;
    private final UserManager userManager;

    @Inject
    public PlayerListener(UserRepository userRepository, UserManager userManager) {
        this.userRepository = userRepository;
        this.userManager = userManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        // 🚀 PILAR 4: Carga Asíncrona (Java 21)
        // Pide los datos a Supabase en un hilo secundario sin dar "lagazos" al entrar.
        userRepository.fetchOrCreateUser(p.getUniqueId(), p.getName()).thenAccept(user -> {
            if (user != null) {
                // Cuando Supabase responde con éxito, guardamos el jugador en la RAM
                userManager.addUserToCache(user);
            } else {
                p.sendMessage("§c❌ Error crítico al cargar tu perfil desde el Vacío. Contacta a un administrador.");
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();

        // 1. Buscamos al jugador en la caché de la RAM
        NexoUser user = userManager.getUserOrNull(p.getUniqueId());

        if (user != null) {
            // 🚀 2. Guardado Asíncrono Seguro
            userRepository.saveUser(user).thenRun(() -> {
                // 3. ANTI DATA-LOSS: Solo lo borramos de la RAM una vez que
                // Supabase nos confirma que los datos llegaron correctamente.
                userManager.removeUserFromCache(p.getUniqueId());
            });
        }
    }
}
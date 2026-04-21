package me.nexo.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 🏛️ Nexo Network - Gestor de Conexiones (Arquitectura Enterprise)
 * Patrón: Cache-First Rapid Reconnect (100% Asíncrono, Zero-Lag, Anti-Wipes).
 */
@Singleton
public class PlayerListener implements Listener {

    // 💉 PILAR 1: Inyección de Dependencias
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

        // 🛡️ 1. REVISIÓN DE CACHÉ (Rapid Reconnect)
        // Si el jugador se desconectó y reconectó rapidísimo, sus datos siguen en la RAM.
        NexoUser cachedUser = userManager.getUserOrNull(p.getUniqueId());
        
        if (cachedUser != null) {
            // ¡Está en la RAM! No hacemos consultas a la BD, entra instantáneamente.
            return;
        }

        // 🚀 2. Carga Asíncrona Normal (Java 21)
        userRepository.fetchOrCreateUser(p.getUniqueId(), p.getName()).thenAccept(user -> {
            if (user != null) {
                userManager.addUserToCache(user);
            } else {
                // 🎨 PILAR 2: Modernización a Kyori Adventure API (Evitamos el uso de '§')
                p.sendMessage(Component.text("❌ Error crítico al cargar tu perfil desde el Vacío. Contacta a un administrador.")
                        .color(NamedTextColor.RED));
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        NexoUser user = userManager.getUserOrNull(p.getUniqueId());

        if (user != null) {
            // 🚀 GUARDADO 100% ASÍNCRONO (Zero-Lag para el servidor)
            userRepository.saveUser(user).thenRun(() -> {
                
                // 🛡️ VALIDACIÓN ANTI-DATA LOSS (Race Condition Fix)
                // Comprobamos si el jugador se volvió a conectar mientras el hilo guardaba en la BD.
                Player jugadorActual = Bukkit.getPlayer(p.getUniqueId());
                
                // Solo lo borramos de la RAM si realmente sigue desconectado.
                if (jugadorActual == null || !jugadorActual.isOnline()) {
                    userManager.removeUserFromCache(p.getUniqueId());
                }
            });
        }
    }
}
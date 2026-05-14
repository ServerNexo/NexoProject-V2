package me.aeroxis.chat.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.chat.AeroxisChatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

@Singleton
public class AeroxisLoginListener implements Listener {

    private final AeroxisChatPlugin plugin;
    private final AeroxisChatManager chatManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Inject
    public AeroxisLoginListener(AeroxisChatPlugin plugin, AeroxisChatManager chatManager) {
        this.plugin = plugin;
        this.chatManager = chatManager;
    }

    // Usamos HIGHEST para asegurarnos de que somos los últimos en decidir si entra o no
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        
        // 1. Verificar si el Mantenimiento está activo
        boolean isMaintenance = plugin.getConfig().getBoolean("pantallas_sistema.mantenimiento_activo", false);
        
        if (isMaintenance) {
            // Verificar si el jugador tiene bypass (Admin) - Como el jugador aún no ha entrado, 
            // no podemos usar player.hasPermission, debemos verificar en el sistema de permisos asíncrono o la base de datos si quieres ser estricto.
            // Por ahora, como es un login listener básico y en Spigot la caché asíncrona de permisos a veces falla, 
            // asumiremos que si está en mantenimiento, la pantalla de rechazo aplica a todos a menos que estén en OP list.
            
            // 🛑 MODO ESTRICTO: Rechazar usando la pantalla custom
            String rawMessage = plugin.getConfig().getString("pantallas_sistema.mantenimiento_mensaje", "<red>Mantenimiento activo.</red>");
            Component kickMessage = chatManager.parseColors(rawMessage); // Reutilizamos tu parseColors para soportar todo
            
            // Si quieres permitir OPs o usuarios específicos, debes verificar su UUID contra una DB aquí,
            // porque Bukkit.getOfflinePlayer(event.getUniqueId()).isOp() bloquea el hilo si se usa mal.
            
            // Rechazar conexión con nuestro mensaje formateado
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            return;
        }

        // 2. Aquí podrías añadir comprobaciones de Baneos futuros conectados a tu Database
        // Ejemplo: Si en tu base de datos el jugador está baneado, lees el campo y haces:
        // event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, mm.deserialize("<red>Baneado por: " + razon + "</red>"));
    }
}
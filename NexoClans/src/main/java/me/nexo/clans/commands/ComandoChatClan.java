package me.nexo.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.core.ClanManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 👥 NexoClans - Sistema de Chat de Facción (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales para distribución masiva de mensajes y Cero Lag I/O.
 */
@Singleton
public class ComandoChatClan implements CommandExecutor {

    private final ClanManager clanManager;
    private final UserManager userManager;

    // 💉 PILAR 3: Inyección de Dependencias Directa
    @Inject
    public ComandoChatClan(ClanManager clanManager, UserManager userManager) {
        this.clanManager = clanManager;
        this.userManager = userManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El chat de clanes es exclusivo para jugadores.");
            return true;
        }

        // Búsqueda en RAM Inyectada (O(1))
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] No perteneces a ninguna facción.");
            return true;
        }

        if (args.length == 0) {
            CrossplayUtils.sendMessage(player, "&#FFAA00[!] Uso: /c <mensaje>");
            return true;
        }

        String mensaje = String.join(" ", args);

        clanManager.getClanFromCache(user.getClanId()).ifPresentOrElse(clan -> {

            // 🌟 FIX: Hilo Virtual.
            // Distribuimos el mensaje de chat de forma asíncrona para que el servidor
            // jamás se congele si hay cientos de jugadores enviando mensajes al mismo tiempo.
            Thread.startVirtualThread(() -> {

                // Formato de alta velocidad sin llamadas de I/O al disco duro
                String formato = "&#FFAA00[Clan] &#E6CCFF" + clan.getName() + " &#555555| &#55FF55" + player.getName() + ": &#FFFFFF" + mensaje;

                // Reparto a los aliados
                for (Player p : Bukkit.getOnlinePlayers()) {
                    NexoUser tUser = userManager.getUserOrNull(p.getUniqueId());

                    if (tUser != null && tUser.hasClan() && tUser.getClanId().equals(user.getClanId())) {
                        CrossplayUtils.sendMessage(p, formato);
                    }
                }
            });

        }, () -> {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] La conexión telepática con tu facción ha fallado.");
        });

        return true;
    }
}
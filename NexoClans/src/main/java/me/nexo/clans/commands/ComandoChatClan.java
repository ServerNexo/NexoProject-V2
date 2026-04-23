package me.nexo.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.core.ClanManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 👥 NexoClans - Sistema de Chat de Facción (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales formales, Inyección Nativa al CommandMap y Cero Lag I/O.
 */
@Singleton
public class ComandoChatClan extends Command {

    private final ClanManager clanManager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 FIX: Gestor formal de Hilos Virtuales para distribución masiva
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 💉 PILAR 1: Inyección de Dependencias Directa y Estructura Nativa
    @Inject
    public ComandoChatClan(ClanManager clanManager, UserManager userManager, CrossplayUtils crossplayUtils) {
        super("c"); // Nombre base del comando inyectado al CommandMap
        this.setAliases(List.of("cc", "clanchat"));
        this.setDescription("Canal de comunicación seguro de la facción.");
        
        this.clanManager = clanManager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El chat de clanes es exclusivo para jugadores.");
            return true;
        }

        // Búsqueda en RAM Inyectada (O(1))
        NexoUser user = userManager.getUserOrNull(player.getUniqueId());

        if (user == null || !user.hasClan()) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] No perteneces a ninguna facción.");
            return true;
        }

        if (args.length == 0) {
            crossplayUtils.sendMessage(player, "&#FFAA00[!] Uso: /c <mensaje>");
            return true;
        }

        String mensaje = String.join(" ", args);

        clanManager.getClanFromCache(user.getClanId()).ifPresentOrElse(clan -> {

            // 🌟 FIX: Distribuimos el mensaje de forma asíncrona pero usando el Executor.
            // El servidor jamás se congelará si hay cientos de jugadores enviando mensajes al mismo tiempo.
            virtualExecutor.submit(() -> {

                // Formato de alta velocidad sin llamadas de I/O al disco duro
                String formato = "&#FFAA00[Clan] &#E6CCFF" + clan.getName() + " &#555555| &#55FF55" + player.getName() + ": &#FFFFFF" + mensaje;

                // Reparto a los aliados
                for (Player p : Bukkit.getOnlinePlayers()) {
                    NexoUser tUser = userManager.getUserOrNull(p.getUniqueId());

                    if (tUser != null && tUser.hasClan() && tUser.getClanId().equals(user.getClanId())) {
                        crossplayUtils.sendMessage(p, formato);
                    }
                }
            });

        }, () -> {
            crossplayUtils.sendMessage(player, "&#FF5555[!] La conexión telepática con tu facción ha fallado.");
        });

        return true;
    }
}
package me.aeroxis.tools.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.tools.AeroxisTools;
import me.aeroxis.tools.managers.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Singleton
public class TpaCommands {

    private final AeroxisTools plugin;
    private final TeleportManager tpManager;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public TpaCommands(AeroxisTools plugin, TeleportManager tpManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.tpManager = tpManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Command({"tpa"})
    @CommandPermission("nexotools.tpa")
    public void tpa(Player sender, Player target) {
        if (sender.equals(target)) {
            crossplayUtils.sendMessage(sender, "&#FF5555❌ No puedes enviarte un TPA a ti mismo.");
            return;
        }

        tpManager.isBlocked(target.getUniqueId(), sender.getUniqueId()).thenAccept(isBlocked -> {
            if (isBlocked) {
                crossplayUtils.sendMessage(sender, "&#FF5555❌ Este jugador tiene bloqueadas tus peticiones.");
                return;
            }

            tpManager.createRequest(sender, target, false);
            crossplayUtils.sendMessage(sender, "&#55FF55✅ Petición de teletransporte enviada a " + target.getName() + ".");
            crossplayUtils.sendMessage(target, "&#FFD700🔔 " + sender.getName() + " quiere teletransportarse hacia ti.\n&#AAAAAAUsa &#55FF55/tpaccept&#AAAAAA o &#FF5555/tpdeny");
        });
    }

    @Command({"tpaccept"})
    @CommandPermission("nexotools.tpa")
    public void tpaccept(Player target) {
        // Para simplificar, aceptamos la petición más reciente (o iteramos si hay varias, pero tomemos el primer sender en este ejemplo rápido)
        Player sender = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (tpManager.getRequest(target, p) != null) {
                sender = p;
                break;
            }
        }

        if (sender == null) {
            crossplayUtils.sendMessage(target, "&#FF5555❌ No tienes peticiones pendientes o han expirado.");
            return;
        }

        TeleportManager.TpaRequest req = tpManager.getRequest(target, sender);
        tpManager.removeRequest(target, sender);

        // Volvemos al Main Thread para teletransportar
        Player finalSender = sender;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (req.isTpaHere) {
                tpManager.saveLastLocation(target);
                target.teleport(finalSender);
            } else {
                tpManager.saveLastLocation(finalSender);
                finalSender.teleport(target);
            }
            crossplayUtils.sendMessage(target, "&#55FF55✅ Petición aceptada.");
            crossplayUtils.sendMessage(finalSender, "&#55FF55✅ " + target.getName() + " aceptó tu petición.");
        });
    }

    @Command({"tpdeny"})
    @CommandPermission("nexotools.tpa")
    public void tpdeny(Player target) {
        crossplayUtils.sendMessage(target, "&#FF5555❌ Has denegado todas las peticiones.");
        for (Player p : Bukkit.getOnlinePlayers()) {
            tpManager.removeRequest(target, p);
        }
    }

    @Command({"tpablock"})
    @CommandPermission("nexotools.tpa")
    public void tpablock(Player target, Player abuser) {
        tpManager.blockPlayer(target.getUniqueId(), abuser.getUniqueId());
        crossplayUtils.sendMessage(target, "&#FF5555🛡️ Has bloqueado a " + abuser.getName() + " de enviarte TPAs.");
    }

    @Command({"back"})
    @CommandPermission("nexotools.back")
    public void back(Player player) {
        org.bukkit.Location loc = tpManager.getLastLocation(player);
        if (loc == null) {
            crossplayUtils.sendMessage(player, "&#FF5555❌ No hay ninguna ubicación previa guardada.");
            return;
        }
        tpManager.saveLastLocation(player); // Guarda donde está ahora antes de volver atrás
        player.teleport(loc);
        crossplayUtils.sendMessage(player, "&#55FF55🔙 Has vuelto a tu ubicación anterior.");
    }
}
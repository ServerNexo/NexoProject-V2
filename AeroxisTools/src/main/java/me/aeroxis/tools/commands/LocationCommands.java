package me.aeroxis.tools.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.tools.AeroxisTools;
import me.aeroxis.tools.managers.LocationManager;
import me.aeroxis.tools.managers.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Singleton
public class LocationCommands {

    private final AeroxisTools plugin;
    private final LocationManager locManager;
    private final TeleportManager tpManager;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public LocationCommands(AeroxisTools plugin, LocationManager locManager, TeleportManager tpManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.locManager = locManager;
        this.tpManager = tpManager;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🌍 WARPS & SPAWN
    // ==========================================
    @Command({"spawn"})
    public void spawn(Player player) {
        teleportToDBLocation(player, "spawn", true);
    }

    @Command({"warp"})
    public void warp(Player player, String name) {
        // En un plugin real revisaríamos el permiso del Warp aquí si lo tiene
        teleportToDBLocation(player, name, true);
    }

    @Command({"setwarp"})
    @CommandPermission("nexotools.admin")
    public void setwarp(Player player, String name, @Optional String permission) {
        locManager.setWarp(name, player.getLocation(), permission).thenAccept(success -> {
            if (success) crossplayUtils.sendMessage(player, "&#55FF55✅ Warp '" + name + "' creado.");
        });
    }

    // ==========================================
    // 🏠 HOMES
    // ==========================================
    @Command({"sethome"})
    @CommandPermission("nexotools.home")
    public void sethome(Player player, @Default("home") String name) {
        locManager.setHome(player.getUniqueId(), name, player.getLocation()).thenAccept(success -> {
            if (success) crossplayUtils.sendMessage(player, "&#55FF55🏠 Hogar '" + name + "' guardado.");
        });
    }

    @Command({"home"})
    @CommandPermission("nexotools.home")
    public void home(Player player, @Default("home") String name) {
        teleportToDBLocation(player, name, false);
    }

    @Command({"homes"})
    @CommandPermission("nexotools.home")
    public void homes(Player player) {
        locManager.getHomes(player.getUniqueId()).thenAccept(homes -> {
            crossplayUtils.sendMessage(player, "&#FFD700🏠 Tus hogares: &#FFFFFF" + String.join(", ", homes));
        });
    }

    // ==========================================
    // 🛠️ UTILS
    // ==========================================
    private void teleportToDBLocation(Player player, String name, boolean isWarp) {
        crossplayUtils.sendMessage(player, "&#AAAAAACargando ubicación...");
        
        var futureLoc = isWarp ? locManager.getWarp(name) : locManager.getHome(player.getUniqueId(), name);
        
        futureLoc.thenAccept(loc -> {
            if (loc == null) {
                crossplayUtils.sendMessage(player, "&#FF5555❌ No se encontró el destino.");
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                tpManager.saveLastLocation(player); // Guardar para el /back
                player.teleport(loc);
                crossplayUtils.sendMessage(player, "&#55FF55⚡ Teletransportado.");
            });
        });
    }
}
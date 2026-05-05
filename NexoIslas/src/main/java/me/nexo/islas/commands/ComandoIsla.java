package me.nexo.islas.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandDatabase;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.data.IslandRole;
import me.nexo.islas.managers.IslandManager;
import me.nexo.islas.menus.IslandCreationMenu; // 🌟 IMPORT NUEVO
import me.nexo.islas.menus.IslandMainMenu;
import me.nexo.islas.menus.IslandTopMenu; // 🌟 IMPORT NUEVO
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎮 Controlador de Comandos de Islas (Lamp Framework Enterprise)
 * Rendimiento: Consultas O(1) en RAM, Menús Asíncronos, Top Global y Sistema de Co-op.
 */
@Singleton
@Command({"is", "isla", "island"})
public class ComandoIsla {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandDatabase db;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();

    @Inject
    public ComandoIsla(NexoIslas plugin, IslandManager islandManager, IslandDatabase db, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.db = db;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🏠 COMANDO BASE: /is (Abre el menú)
    // ==========================================
    @DefaultFor({"~"})
    public void defaultCommand(Player player) {
        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());

        if (profile == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Aún no eres dueño de una isla.");
            crossplayUtils.sendMessage(player, "&#FFAA00💡 Usa &#00f5ff/is create &#FFAA00para materializar tu imperio.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
            new IslandMainMenu(player, crossplayUtils, plugin, islandManager, profile).open();
        }
    }

    // ==========================================
    // 🏗️ CREAR ISLA: /is create
    // ==========================================
    @Subcommand("create")
    public void createIsland(Player player) {
        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());

        if (profile != null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] ¡Ya posees una isla en las coordenadas celestiales!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 🌟 FIX: Abre el menú de selección de plantillas
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        new IslandCreationMenu(player, crossplayUtils, plugin, islandManager, db).open();
    }

    // ==========================================
    // 🚶 IR A LA ISLA: /is home
    // ==========================================
    @Subcommand("home")
    public void homeIsland(Player player) {
        islandManager.loadIslandAsync(player);
    }

    // ==========================================
    // ✉️ INVITAR JUGADOR: /is invite <jugador>
    // ==========================================
    @Subcommand("invite")
    public void invitePlayer(Player player, Player target) {
        if (player.equals(target)) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] No puedes invitarte a ti mismo.");
            return;
        }

        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());
        if (profile == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] No tienes una isla activa.");
            return;
        }
        if (!profile.getOwnerId().equals(player.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Solo el líder absoluto puede invitar miembros.");
            return;
        }
        if (profile.getMembers().size() >= profile.getRealMemberLimit()) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Has alcanzado el límite de miembros en tu isla.");
            return;
        }
        if (profile.isMember(target.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Ese jugador ya pertenece a tu imperio.");
            return;
        }

        pendingInvites.put(target.getUniqueId(), player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        crossplayUtils.sendMessage(player, "&#55FF55[✓] Invitación enviada a " + target.getName() + ".");

        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        crossplayUtils.sendMessage(target, "&#00f5ff✧ &#55FF55¡Has recibido una invitación para unirte a la isla de " + player.getName() + "!");
        crossplayUtils.sendMessage(target, "&#FFAA00💡 Usa &#00f5ff/is accept &#FFAA00para unirte a su imperio.");
    }

    // ==========================================
    // ✅ ACEPTAR INVITACIÓN: /is accept
    // ==========================================
    @Subcommand("accept")
    public void acceptInvite(Player player) {
        UUID ownerId = pendingInvites.get(player.getUniqueId());
        if (ownerId == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] No tienes invitaciones pendientes.");
            return;
        }

        IslandProfile ownProfile = islandManager.getIslandByOwner(player.getUniqueId());
        if (ownProfile != null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Debes abandonar tu isla actual para unirte a otra.");
            return;
        }

        IslandProfile targetProfile = islandManager.getIslandByOwner(ownerId);
        if (targetProfile == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] La isla a la que fuiste invitado no está disponible.");
            pendingInvites.remove(player.getUniqueId());
            return;
        }

        if (targetProfile.getMembers().size() >= targetProfile.getRealMemberLimit()) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Lo sentimos, el equipo de esa isla se ha llenado.");
            pendingInvites.remove(player.getUniqueId());
            return;
        }

        targetProfile.addMember(player.getUniqueId(), IslandRole.MEMBER);
        pendingInvites.remove(player.getUniqueId());
        islandManager.saveIslandProfileAsync(targetProfile);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        crossplayUtils.sendMessage(player, "&#55FF55[✓] ¡Te has unido a la isla con éxito!");

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null && owner.isOnline()) {
            crossplayUtils.sendMessage(owner, "&#55FF55[✓] " + player.getName() + " se ha unido a tu isla.");
        }

        Bukkit.getRegionScheduler().run(plugin, player.getLocation(), task -> {
            islandManager.loadIslandAsync(player);
        });
    }

    // ==========================================
    // 🥾 EXPULSAR JUGADOR: /is kick <jugador>
    // ==========================================
    @Subcommand("kick")
    public void kickPlayer(Player player, OfflinePlayer target) {
        if (player.getUniqueId().equals(target.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] No puedes expulsarte a ti mismo.");
            return;
        }

        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());
        if (profile == null || !profile.getOwnerId().equals(player.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Solo el líder absoluto puede expulsar miembros.");
            return;
        }

        if (!profile.getMembers().containsKey(target.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Ese jugador no es miembro de tu isla.");
            return;
        }

        profile.removeMember(target.getUniqueId());
        islandManager.saveIslandProfileAsync(profile);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
        crossplayUtils.sendMessage(player, "&#FF5555[✓] Has expulsado a " + target.getName() + " de tu isla.");

        if (target.isOnline()) {
            Player targetPlayer = (Player) target;
            crossplayUtils.sendMessage(targetPlayer, "&#FF5555[!] Has sido expulsado de la isla de " + player.getName() + ".");
            Bukkit.getRegionScheduler().run(plugin, targetPlayer.getLocation(), task -> {
                targetPlayer.performCommand("spawn");
            });
        }
    }

    // ==========================================
    // 🏆 TOP DE ISLAS: /is top
    // ==========================================
    @Subcommand("top")
    public void showTop(Player player) {
        crossplayUtils.sendMessage(player, "&#FFAA00⏳ Sincronizando datos con el Nexo Central...");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1f, 2f);

        // 🌟 RENDIMIENTO AAA: Solicitamos ambas listas asincronamente
        var futureLevel = db.getTopIslandsByLevel();
        var futureValue = db.getTopIslandsByValue();

        futureLevel.thenAcceptBoth(futureValue, (topLevel, topValue) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
                // Abrimos la GUI visual pasándole los datos ya calculados
                new IslandTopMenu(player, crossplayUtils, topLevel, topValue).open();
            });
        }).exceptionally(ex -> {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Error sincronizando el Leaderboard.");
            ex.printStackTrace();
            return null;
        });
    }

    // ==========================================
    // 👑 COMANDOS DE ADMINISTRADOR: /is admin
    // ==========================================
    @Subcommand("admin reset")
    @CommandPermission("nexo.islas.admin")
    public void adminReset(Player player) {
        crossplayUtils.sendMessage(player, "&#FFAA00[⚠️] Panel de administración en construcción.");
    }
}
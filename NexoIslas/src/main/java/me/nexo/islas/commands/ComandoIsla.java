package me.nexo.islas.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandDatabase;
import me.nexo.islas.managers.IslandManager;
import me.nexo.islas.menus.IslandMainMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎮 Controlador de Comandos de Islas (Lamp Framework)
 * Incluye gestión de Co-op (Invite, Accept, Kick).
 */
@Singleton
@Command({"is", "isla", "island"})
public class ComandoIsla {

    private final NexoIslas plugin;
    private final IslandManager islandManager;
    private final IslandDatabase db;
    private final CrossplayUtils crossplayUtils;

    // 🌟 Caché temporal de invitaciones: Map<InvitadoUUID, DueñoUUID>
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
        db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (profile == null) {
                    player.sendMessage("§c❌ Aún no eres dueño de una isla.");
                    player.sendMessage("§e💡 Usa §b/is create §epara materializar tu imperio.");
                } else {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1f, 1f);
                    new IslandMainMenu(player, crossplayUtils, plugin, islandManager, profile).open();
                }
            });
        });
    }

    // ==========================================
    // 🏗️ CREAR ISLA: /is create
    // ==========================================
    @Subcommand("create")
    public void createIsland(Player player) {
        db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
            if (profile != null) {
                player.sendMessage("§c❌ ¡Ya posees una isla en las coordenadas celestiales!");
                return;
            }
            player.sendMessage("§e⏳ Generando tu micromundo... Por favor espera.");
            islandManager.createIslandAsync(player);
        });
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
            player.sendMessage("§c❌ No puedes invitarte a ti mismo.");
            return;
        }

        db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (profile == null) {
                    player.sendMessage("§c❌ No tienes una isla.");
                    return;
                }
                if (!profile.getOwnerId().equals(player.getUniqueId())) {
                    player.sendMessage("§c❌ Solo el dueño de la isla puede invitar a otros.");
                    return;
                }
                if (profile.getMembers().size() >= profile.getMemberLimit()) {
                    player.sendMessage("§c❌ Has alcanzado el límite de miembros en tu isla.");
                    return;
                }
                if (profile.isMember(target.getUniqueId())) {
                    player.sendMessage("§c❌ Ese jugador ya pertenece a tu isla.");
                    return;
                }

                pendingInvites.put(target.getUniqueId(), player.getUniqueId());
                player.sendMessage("§a✅ Invitación enviada a §e" + target.getName() + "§a.");

                target.sendMessage("§a📬 ¡Has recibido una invitación para unirte a la isla de §e" + player.getName() + "§a!");
                target.sendMessage("§e💡 Usa §b/is accept §epara unirte a su imperio.");
            });
        });
    }

    // ==========================================
    // ✅ ACEPTAR INVITACIÓN: /is accept
    // ==========================================
    @Subcommand("accept")
    public void acceptInvite(Player player) {
        UUID ownerId = pendingInvites.get(player.getUniqueId());
        if (ownerId == null) {
            player.sendMessage("§c❌ No tienes invitaciones pendientes.");
            return;
        }

        // Verificamos que el invitado no tenga su propia isla
        db.loadIsland(player.getUniqueId()).thenAccept(ownProfile -> {
            if (ownProfile != null) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§c❌ Debes abandonar o borrar tu isla actual para unirte a otra."));
                return;
            }

            db.loadIsland(ownerId).thenAccept(profile -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (profile == null) {
                        player.sendMessage("§c❌ La isla a la que fuiste invitado ya no existe.");
                        pendingInvites.remove(player.getUniqueId());
                        return;
                    }
                    if (profile.getMembers().size() >= profile.getMemberLimit()) {
                        player.sendMessage("§c❌ Lo sentimos, la isla se ha llenado.");
                        pendingInvites.remove(player.getUniqueId());
                        return;
                    }

                    // Lo agregamos y guardamos asíncronamente
                    profile.addMember(player.getUniqueId());
                    CompletableFuture.runAsync(() -> db.saveIslandSync(profile));
                    pendingInvites.remove(player.getUniqueId());

                    player.sendMessage("§a✅ ¡Te has unido a la isla con éxito!");
                    Player owner = Bukkit.getPlayer(ownerId);
                    if (owner != null && owner.isOnline()) {
                        owner.sendMessage("§a✅ §e" + player.getName() + " §ase ha unido a tu isla.");
                    }

                    // Lo teletransportamos a su nueva casa
                    islandManager.loadIslandAsync(player);
                });
            });
        });
    }

    // ==========================================
    // 🥾 EXPULSAR JUGADOR: /is kick <jugador>
    // ==========================================
    @Subcommand("kick")
    public void kickPlayer(Player player, OfflinePlayer target) {
        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("§c❌ No puedes expulsarte a ti mismo.");
            return;
        }

        db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (profile == null || !profile.getOwnerId().equals(player.getUniqueId())) {
                    player.sendMessage("§c❌ Solo el dueño puede expulsar miembros.");
                    return;
                }
                if (!profile.getMembers().contains(target.getUniqueId())) {
                    player.sendMessage("§c❌ Ese jugador no es miembro de tu isla.");
                    return;
                }

                // Lo quitamos y guardamos asíncronamente
                profile.removeMember(target.getUniqueId());
                CompletableFuture.runAsync(() -> db.saveIslandSync(profile));

                player.sendMessage("§a✅ Has expulsado a §e" + target.getName() + " §ade tu isla.");

                // Si está conectado, le avisamos y lo sacamos de la isla
                if (target.isOnline()) {
                    Player targetPlayer = (Player) target;
                    targetPlayer.sendMessage("§c❌ Has sido expulsado de la isla de §e" + player.getName() + "§c.");

                    if (targetPlayer.getWorld().getName().equals("nexo_islas_world")) {
                        targetPlayer.performCommand("spawn"); // Lo mandamos al spawn
                    }
                }
            });
        });
    }

    // ==========================================
    // 👑 COMANDOS DE ADMINISTRADOR: /is admin
    // ==========================================
    @Subcommand("admin reset")
    @CommandPermission("nexo.islas.admin")
    public void adminReset(Player player) {
        player.sendMessage("§c⚠️ Panel de administración en construcción.");
    }
}
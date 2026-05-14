package me.aeroxis.islas.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.islas.AeroxisIslas;
import me.aeroxis.islas.data.IslandDatabase;
import me.aeroxis.islas.data.IslandProfile;
import me.aeroxis.islas.data.IslandRole;
import me.aeroxis.islas.managers.IslandManager;
import me.aeroxis.islas.menus.IslandCreationMenu;
import me.aeroxis.islas.menus.IslandMainMenu;
import me.aeroxis.islas.menus.IslandTopMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎮 Controlador de Comandos de Islas (Lamp Framework Enterprise)
 * Rendimiento: Consultas O(1) en RAM, Menús Asíncronos, Top Global y Sistema de Co-op.
 */
@Singleton
@Command({"is", "isla", "island"})
public class ComandoIsla {

    private final AeroxisIslas plugin;
    private final IslandManager islandManager;
    private final IslandDatabase db;
    private final CrossplayUtils crossplayUtils;

    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();
    private final Set<UUID> pendingDeletion = ConcurrentHashMap.newKeySet();

    @Inject
    public ComandoIsla(AeroxisIslas plugin, IslandManager islandManager, IslandDatabase db, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.db = db;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 🏠 COMANDO BASE: /is
    // ==========================================
    @DefaultFor({"~"})
    public void defaultCommand(Player player) {
        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());

        if (profile != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
            new IslandMainMenu(player, crossplayUtils, plugin, islandManager, profile).open();
        } else {
            // 🌟 FIX AUDITORÍA: Si la RAM está vacía (Ej. se acaba de conectar), comprobamos PostgreSQL
            crossplayUtils.sendMessage(player, "&#FFAA00⏳ Sincronizando con el Nexo Central...");
            db.loadIsland(player.getUniqueId()).thenAccept(dbProfile -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (dbProfile == null) {
                        crossplayUtils.sendMessage(player, "&#FF5555[x] Aún no eres dueño de una isla.");
                        crossplayUtils.sendMessage(player, "&#FFAA00💡 Usa &#00f5ff/is create &#FFAA00para materializar tu imperio.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } else {
                        // Guardamos el perfil cargado en la RAM para futuros comandos rápidos
                        islandManager.cacheIsland(dbProfile);
                        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
                        new IslandMainMenu(player, crossplayUtils, plugin, islandManager, dbProfile).open();
                    }
                });
            });
        }
    }

    // ==========================================
    // 🏗️ CREAR ISLA: /is create
    // ==========================================
    @Subcommand("create")
    public void createIsland(Player player) {
        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());

        if (profile != null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] ¡Ya posees una isla!");
            return;
        }

        // 🌟 FIX AUDITORÍA: Misma seguridad para la creación de isla
        db.loadIsland(player.getUniqueId()).thenAccept(dbProfile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (dbProfile != null) {
                    islandManager.cacheIsland(dbProfile);
                    crossplayUtils.sendMessage(player, "&#FF5555[x] ¡Ya posees una isla en las coordenadas celestiales!");
                } else {
                    new IslandCreationMenu(player, crossplayUtils, plugin, islandManager, db).open();
                }
            });
        });
    }

    // ==========================================
    // 🚶 IR A LA ISLA: /is home o /is go
    // ==========================================
    @Subcommand({"home", "go"}) // 🌟 FIX: Ahora ambos comandos funcionan igual
    public void homeIsland(Player player) {
        islandManager.loadIslandAsync(player);
    }

    // ==========================================
    // 🧨 DESTRUIR ISLA: /is delete
    // ==========================================
    @Subcommand("delete")
    public void deleteIslandCommand(Player player) {
        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());

        if (profile == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] No tienes ninguna isla activa.");
            return;
        }

        pendingDeletion.add(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);
        crossplayUtils.sendMessage(player, "&#FF5555[⚠️] ¡ESTÁS A PUNTO DE DESTRUIR TU ISLA PARA SIEMPRE!");
        crossplayUtils.sendMessage(player, "&#FFAA00💡 Escribe &#00f5ff/is confirm &#FFAA00para destruir tu imperio.");

        // Expirar en 20 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> pendingDeletion.remove(player.getUniqueId()), 20L * 20);
    }

    @Subcommand("confirm")
    public void confirmCommand(Player player) {
        if (!pendingDeletion.contains(player.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] No hay ninguna eliminación pendiente.");
            return;
        }

        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());
        if (profile != null) {
            islandManager.deleteIslandAsync(profile, player);
        }
        pendingDeletion.remove(player.getUniqueId());
    }

    // ==========================================
    // ✉️ INVITAR JUGADOR: /is invite <jugador>
    // ==========================================
    @Subcommand("invite")
    public void invitePlayer(Player player, Player target) {
        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());
        if (profile == null || !profile.getOwnerId().equals(player.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] Solo el líder puede invitar.");
            return;
        }
        pendingInvites.put(target.getUniqueId(), player.getUniqueId());
        crossplayUtils.sendMessage(player, "&#55FF55[✓] Invitación enviada.");
        crossplayUtils.sendMessage(target, "&#00f5ff✧ ¡Has recibido una invitación de " + player.getName() + "!");
    }

    // ==========================================
    // ✅ ACEPTAR: /is accept
    // ==========================================
    @Subcommand("accept")
    public void acceptInvite(Player player) {
        UUID ownerId = pendingInvites.remove(player.getUniqueId());
        if (ownerId == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[x] No tienes invitaciones.");
            return;
        }

        IslandProfile targetProfile = islandManager.getIslandByOwner(ownerId);
        if (targetProfile != null) {
            targetProfile.addMember(player.getUniqueId(), IslandRole.MEMBER);
            islandManager.saveIslandProfileAsync(targetProfile);
            crossplayUtils.sendMessage(player, "&#55FF55[✓] ¡Te has unido!");
        }
    }

    // ==========================================
    // 🥾 EXPULSAR: /is kick <jugador>
    // ==========================================
    @Subcommand("kick")
    public void kickPlayer(Player player, OfflinePlayer target) {
        IslandProfile profile = islandManager.getIslandByOwner(player.getUniqueId());
        if (profile != null && profile.getOwnerId().equals(player.getUniqueId())) {
            profile.removeMember(target.getUniqueId());
            islandManager.saveIslandProfileAsync(profile);
            crossplayUtils.sendMessage(player, "&#FF5555[✓] Jugador expulsado.");
        }
    }

    // ==========================================
    // 🏆 TOP: /is top
    // ==========================================
    @Subcommand("top")
    public void showTop(Player player) {
        db.getTopIslandsByLevel().thenAccept(topLevel -> {
            db.getTopIslandsByValue().thenAccept(topValue -> {
                Bukkit.getScheduler().runTask(plugin, () ->
                        new IslandTopMenu(player, crossplayUtils, topLevel, topValue).open()
                );
            });
        });
    }
}
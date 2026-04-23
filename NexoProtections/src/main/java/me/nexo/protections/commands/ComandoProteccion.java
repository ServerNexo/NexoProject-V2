package me.nexo.protections.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🛡️ NexoProtections - Comando Principal (Arquitectura Enterprise)
 * Rendimiento: 100% Lamp, Inyección Pura, Parseo Moderno y Teletransporte Asíncrono.
 */
@Singleton
@Command({"nexo", "monolito", "proteccion"})
public class ComandoProteccion {

    private final NexoProtections plugin;
    private final ConfigManager configManager;
    private final ClaimManager claimManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    // 💉 PILAR 1: Inyección Directa y Limpia
    @Inject
    public ComandoProteccion(NexoProtections plugin, ConfigManager configManager, ClaimManager claimManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.claimManager = claimManager;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 COMANDO: /nexo (Dar el Monolito)
    @DefaultFor("~")
    @CommandPermission("nexo.admin")
    public void giveMonolith(Player player) {
        var stone = new ItemStack(Material.LODESTONE);
        var meta = stone.getItemMeta();
        
        if (meta != null) {
            // 🌟 FIX COLOR: Parseo directo con la utilidad de tu Ecosistema
            meta.displayName(crossplayUtils.parseCrossplay(player, configManager.getMessages().mensajes().items().selloAbismoNombre()));

            meta.lore(configManager.getMessages().mensajes().items().selloAbismoLore().stream()
                    .map(line -> crossplayUtils.parseCrossplay(player, line))
                    .toList());

            var key = new NamespacedKey(plugin, "is_protection_stone");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            stone.setItemMeta(meta);
        }

        player.getInventory().addItem(stone);
        crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().ritualConcedido());
    }

    // 🌟 COMANDO: /nexo reload
    @Subcommand("reload")
    @CommandPermission("nexo.admin")
    public void reloadSystem(Player player) {
        // Ejecuta el reload desde la clase principal (recarga YAML y RAM)
        plugin.reloadSystem();
        crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().abismoDespierta());
    }

    // 🌟 COMANDO: /nexo home (Viaje a la base)
    @Subcommand("home")
    public void goHome(Player player) {
        ProtectionStone myStone = null;

        // Búsqueda en caché RAM O(n) rápida
        for (ProtectionStone stone : claimManager.getAllStones().values()) {
            if (stone.getOwnerId().equals(player.getUniqueId())) {
                myStone = stone;
                break;
            }
        }

        if (myStone == null) {
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinMonolitoHome());
            return;
        }

        var box = myStone.getBox();
        int centerX = (box.minX() + box.maxX()) / 2;
        int centerZ = (box.minZ() + box.maxZ()) / 2;
        var world = Bukkit.getWorld(box.world());

        if (world != null) {
            int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
            var tpLoc = new Location(world, centerX + 0.5, y, centerZ + 0.5);

            // Paper method (Mucho más rápido y no congela el servidor al cargar chunks)
            player.teleportAsync(tpLoc).thenAccept(success -> {
                if (success) {
                    crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().viajeEspacial());
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                }
            });
        }
    }

    // 🌟 COMANDO: /nexo ver (Revelar Fronteras)
    @Subcommand("ver")
    public void verFronteras(Player player) {
        var stone = claimManager.getStoneAt(player.getLocation());
        if (stone == null) {
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().fueraFronteras());
            return;
        }

        var box = stone.getBox();
        double y = player.getLocation().getY() + 1.0;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            player.spawnParticle(Particle.PORTAL, x + 0.5, y, box.minZ() + 0.5, 3, 0, 0, 0, 0);
            player.spawnParticle(Particle.PORTAL, x + 0.5, y, box.maxZ() + 0.5, 3, 0, 0, 0, 0);
        }
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            player.spawnParticle(Particle.PORTAL, box.minX() + 0.5, y, z + 0.5, 3, 0, 0, 0, 0);
            player.spawnParticle(Particle.PORTAL, box.maxX() + 0.5, y, z + 0.5, 3, 0, 0, 0, 0);
        }

        crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().visionVacio());
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
    }

    // 🌟 COMANDO: /nexo trust <jugador>
    @Subcommand("trust")
    public void trustPlayer(Player player, Player target) {
        var stone = claimManager.getStoneAt(player.getLocation());
        
        if (stone == null || !stone.getOwnerId().equals(player.getUniqueId())) {
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().herejiaTrust());
            return;
        }

        if (target == null) {
            crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().almaOffline());
            return;
        }

        stone.addFriend(target.getUniqueId());
        
        // 🛡️ Guardado asíncrono
        claimManager.saveStoneDataAsync(stone);

        crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().pactoForjadoOwner()
                .replace("%target%", target.getName()));

        crossplayUtils.sendMessage(target, configManager.getMessages().mensajes().exito().pactoForjadoTarget()
                .replace("%owner%", player.getName()));
    }
}
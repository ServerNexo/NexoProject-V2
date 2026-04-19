package me.nexo.protections.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.ConfigManager;
import me.nexo.protections.core.ClaimBox;
import me.nexo.protections.core.ProtectionStone;
import me.nexo.protections.managers.ClaimManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🛡️ NexoProtections - Comando Principal (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoProteccion extends Command {

    private final NexoProtections plugin;
    private final ConfigManager configManager;
    private final ClaimManager claimManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoProteccion(NexoProtections plugin, ConfigManager configManager, ClaimManager claimManager) {
        super("nexo"); // 🌟 Nombre nativo
        this.setAliases(List.of("monolito", "proteccion")); // Alias extra

        this.plugin = plugin;
        this.configManager = configManager;
        this.claimManager = claimManager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] La consola no puede tener un Monolito de Protección.");
            return true;
        }

        // 🌟 COMANDO: /nexo (Dar el Monolito)
        if (args.length == 0) {
            if (player.hasPermission("nexo.admin")) {
                giveMonolith(player);
            } else {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /nexo <home|ver|trust>");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 🌟 OPTIMIZACIÓN: Switch de Java 21 para ruteo instantáneo
        switch (subCommand) {
            case "reload" -> {
                if (player.hasPermission("nexo.admin")) {
                    configManager.reloadMessages();
                    CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().abismoDespierta());
                } else {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] El Vacío te rechaza (Sin Permisos).");
                }
            }
            case "home" -> goHome(player);
            case "ver" -> verFronteras(player);
            case "trust" -> {
                if (args.length < 2) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /nexo trust <jugador>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                trustPlayer(player, target);
            }
            default -> {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Comando desconocido. Usa /nexo <home|ver|trust>");
            }
        }
        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        List<String> subcommands = new ArrayList<>(List.of("home", "ver", "trust"));
        if (sender.hasPermission("nexo.admin")) {
            subcommands.add("reload");
        }

        if (args.length == 1) {
            return subcommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }

    // ==========================================
    // 💡 LÓGICA DE NEGOCIO SEPARADA
    // ==========================================

    private void giveMonolith(Player player) {
        ItemStack stone = new ItemStack(Material.LODESTONE);
        ItemMeta meta = stone.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(
                    configManager.getMessages().mensajes().items().selloAbismoNombre()
            ));

            meta.lore(configManager.getMessages().mensajes().items().selloAbismoLore().stream()
                    .map(line -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                    .toList());

            NamespacedKey key = new NamespacedKey(plugin, "is_protection_stone");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            stone.setItemMeta(meta);
        }

        player.getInventory().addItem(stone);
        CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().ritualConcedido());
    }

    private void goHome(Player player) {
        ProtectionStone myStone = null;

        for (ProtectionStone stone : claimManager.getAllStones().values()) {
            if (stone.getOwnerId().equals(player.getUniqueId())) {
                myStone = stone;
                break;
            }
        }

        if (myStone == null) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().sinMonolitoHome());
            return;
        }

        ClaimBox box = myStone.getBox();
        int centerX = (box.minX() + box.maxX()) / 2;
        int centerZ = (box.minZ() + box.maxZ()) / 2;
        World world = Bukkit.getWorld(box.world());

        if (world != null) {
            int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
            Location tpLoc = new Location(world, centerX + 0.5, y, centerZ + 0.5);

            player.teleportAsync(tpLoc); // Paper method (Mucho más rápido y seguro)
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().viajeEspacial());
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        }
    }

    private void verFronteras(Player player) {
        ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
        if (stone == null) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().fueraFronteras());
            return;
        }

        ClaimBox box = stone.getBox();
        double y = player.getLocation().getY() + 1.0;

        for (int x = box.minX(); x <= box.maxX(); x++) {
            player.spawnParticle(Particle.PORTAL, x + 0.5, y, box.minZ() + 0.5, 3, 0, 0, 0, 0);
            player.spawnParticle(Particle.PORTAL, x + 0.5, y, box.maxZ() + 0.5, 3, 0, 0, 0, 0);
        }
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            player.spawnParticle(Particle.PORTAL, box.minX() + 0.5, y, z + 0.5, 3, 0, 0, 0, 0);
            player.spawnParticle(Particle.PORTAL, box.maxX() + 0.5, y, z + 0.5, 3, 0, 0, 0, 0);
        }

        CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().visionVacio());
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
    }

    private void trustPlayer(Player player, Player target) {
        ProtectionStone stone = claimManager.getStoneAt(player.getLocation());
        if (stone == null || !stone.getOwnerId().equals(player.getUniqueId())) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().herejiaTrust());
            return;
        }

        if (target == null) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().errores().almaOffline());
            return;
        }

        stone.addFriend(target.getUniqueId());
        claimManager.saveStoneDataAsync(stone);

        CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().pactoForjadoOwner()
                .replace("%target%", target.getName()));

        CrossplayUtils.sendMessage(target, configManager.getMessages().mensajes().exito().pactoForjadoTarget()
                .replace("%owner%", player.getName()));
    }
}
package me.nexo.minions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoItems;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.ConfigManager;
import me.nexo.minions.data.MinionKeys;
import me.nexo.minions.data.MinionType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🤖 NexoMinions - Comando Principal (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoMinion extends Command {

    private final NexoMinions plugin;
    private final ConfigManager configManager;

    @Inject
    public ComandoMinion(NexoMinions plugin, ConfigManager configManager) {
        super("minion"); // 🌟 Nombre nativo base
        this.setAliases(List.of("minions")); // Alias nativos
        this.setPermission("nexominions.admin"); // Permiso base

        this.plugin = plugin;
        this.configManager = configManager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 🌟 FIX: 1. Primero validamos que sea un jugador físico para evitar el error del "sender" nulo.
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] El terminal requiere un operario humano para invocar minions.");
            return true;
        }

        // 🌟 FIX: 2. Ahora que sabemos que es un 'player', validamos permisos y enviamos mensajes seguros.
        if (!player.hasPermission("nexominions.admin")) {
            CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().sinPermiso());
            return true;
        }

        if (args.length == 0) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /minion <reload|give>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 🌟 OPTIMIZACIÓN: Switch de Java 21
        switch (subCommand) {
            case "reload" -> {
                configManager.reloadMessages();
                plugin.getTiersConfig().cargarConfig();
                plugin.getUpgradesConfig().cargarConfig();
                CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().reloadExito());
            }
            case "give" -> {
                if (args.length < 3) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /minion give <jugador> <tipo> [nivel]");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().jugadorOffline());
                    return true;
                }

                MinionType type;
                try {
                    type = MinionType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Tipo de minion inválido o no reconocido.");
                    return true;
                }

                int tier = 1; // Nivel por defecto
                if (args.length >= 4) {
                    try {
                        tier = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().nivelInvalido());
                        return true;
                    }
                }

                if (tier < 1 || tier > 12) {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().nivelInvalido());
                    return true;
                }

                var itemFactory = NexoItems.itemFromId(type.getNexoModelID());
                if (itemFactory == null) {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().selloNoExiste().replace("%id%", type.getNexoModelID()));
                    return true;
                }

                ItemStack minionItem = itemFactory.build();

                if (minionItem == null || minionItem.getType().isAir()) {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().materiaVacia());
                    return true;
                }

                ItemMeta meta = minionItem.getItemMeta();
                if (meta != null) {
                    String nombre = configManager.getMessages().comandos().itemNombre()
                            .replace("%type%", type.getDisplayName())
                            .replace("%tier%", String.valueOf(tier));

                    meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(nombre));

                    List<net.kyori.adventure.text.Component> lore = configManager.getMessages().comandos().itemLore().stream()
                            .map(line -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                            .collect(Collectors.toList());
                    meta.lore(lore);

                    meta.getPersistentDataContainer().set(MinionKeys.TYPE, PersistentDataType.STRING, type.name());
                    meta.getPersistentDataContainer().set(MinionKeys.TIER, PersistentDataType.INTEGER, tier);

                    minionItem.setItemMeta(meta);
                } else {
                    CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().falloNbt());
                    return true;
                }

                target.getInventory().addItem(minionItem);
                CrossplayUtils.sendMessage(player, configManager.getMessages().comandos().invocacionAprobada()
                        .replace("%type%", type.getDisplayName())
                        .replace("%tier%", String.valueOf(tier))
                        .replace("%target%", target.getName()));
                CrossplayUtils.sendMessage(target, configManager.getMessages().comandos().pactoForjado());
            }
            default -> CrossplayUtils.sendMessage(player, "&#FF5555[!] Comando desconocido. Usa /minion <reload|give>");
        }

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        if (!sender.hasPermission("nexominions.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("give", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(MinionType.values())
                    .map(MinionType::name)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12").stream()
                    .filter(s -> s.startsWith(args[3]))
                    .toList();
        }

        return Collections.emptyList();
    }
}
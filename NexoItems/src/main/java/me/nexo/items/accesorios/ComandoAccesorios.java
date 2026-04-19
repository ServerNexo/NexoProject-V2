package me.nexo.items.accesorios;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🎒 NexoItems - Comando Principal de Accesorios (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoAccesorios extends Command {

    private final NexoItems plugin;
    private final AccesoriosManager manager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoAccesorios(NexoItems plugin, AccesoriosManager manager) {
        super("accesorios"); // 🌟 Nombre nativo base
        this.setAliases(List.of("accesorio", "accs", "talismans", "talismanes")); // Alias extra

        this.plugin = plugin;
        this.manager = manager; // Inyectamos el manager directo a la vena
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] Comando solo disponible para jugadores en el plano físico.");
            return true;
        }

        // 1. Abrir Bóveda (/accesorios)
        if (args.length == 0) {
            manager.abrirBolsa(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 🌟 Switch de Java 21 para ruteo instantáneo
        switch (subCommand) {
            case "help" -> {
                CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
                CrossplayUtils.sendMessage(player, "&#ff00ff<bold>💍 SISTEMA DE ACCESORIOS</bold>");
                CrossplayUtils.sendMessage(player, "&#00f5ff/accesorios &#E6CCFF- Abre tu bóveda de accesorios.");

                if (player.hasPermission("nexo.admin")) {
                    CrossplayUtils.sendMessage(player, "&#00f5ff/accesorios give <jugador> <id> &#E6CCFF- Concede un accesorio.");
                }

                CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            }
            case "give" -> {
                if (!player.hasPermission("nexo.admin")) {
                    CrossplayUtils.sendMessage(player, "&#FF3366[!] El Vacío rechaza tu petición (Sin Permisos).");
                    return true;
                }

                if (args.length < 3) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso incorrecto: /accesorios give <jugador> <id>");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    CrossplayUtils.sendMessage(player, "&#FF3366[!] Ese jugador no se encuentra en este plano.");
                    return true;
                }

                String accId = args[2].toLowerCase();
                ItemStack item = manager.generarAccesorio(accId);

                if (item == null) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Anomalía detectada: El accesorio '" + accId + "' no existe en el registro.");
                    return true;
                }

                target.getInventory().addItem(item);
                CrossplayUtils.sendMessage(target, "&#00f5ff[📦] Has recibido un nuevo Accesorio de poder.");
                CrossplayUtils.sendMessage(player, "&#00f5ff[📦] Accesorio entregado con éxito a " + target.getName() + ".");
            }
            default -> {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Comando desconocido. Usa /accesorios help");
            }
        }

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            sub.add("help");
            if (sender.hasPermission("nexo.admin")) {
                sub.add("give");
            }
            return sub.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("nexo.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("nexo.admin")) {
            return List.of("<id_accesorio>");
        }

        return Collections.emptyList();
    }
}
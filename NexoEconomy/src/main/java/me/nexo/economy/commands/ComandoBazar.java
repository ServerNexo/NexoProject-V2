package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.bazar.BazaarManager;
import me.nexo.economy.bazar.BazaarMenu;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 💰 NexoEconomy - Comando del Bazar (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoBazar extends Command {

    private final NexoEconomy plugin;
    private final BazaarManager bazaarManager;

    // 🌟 Listas estáticas inmutables para un autocompletado Zero-Lag
    private static final List<String> SUB_COMMANDS = List.of("help", "claim", "sell", "buy");
    private static final List<String> SUGGESTED_PRICES = List.of("10", "100", "500", "1000");
    private static final List<String> SUGGESTED_AMOUNTS = List.of("1", "16", "32", "64");

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoBazar(NexoEconomy plugin, BazaarManager bazaarManager) {
        super("bazar"); // 🌟 Nombre nativo
        this.setAliases(List.of("bazaar", "bz")); // Alias directos
        this.plugin = plugin;
        this.bazaarManager = bazaarManager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] La consola no puede operar en el Bazar.");
            return true;
        }

        // Abre el menú si no hay argumentos
        if (args.length == 0) {
            new BazaarMenu(player, plugin).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 🌟 OPTIMIZACIÓN: Switch de Java 21 para ruteo instantáneo
        switch (subCommand) {
            case "help" -> {
                CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
                CrossplayUtils.sendMessage(player, "&#FFAA00⚖ <bold>COMANDOS DEL BAZAR</bold>");
                CrossplayUtils.sendMessage(player, "&#00f5ff/bazar &#E6CCFF- Abre el menú interactivo.");
                CrossplayUtils.sendMessage(player, "&#00f5ff/bazar sell <precio_ud> &#E6CCFF- Vende el ítem de tu mano.");
                CrossplayUtils.sendMessage(player, "&#00f5ff/bazar buy <item> <cant> <precio_ud> &#E6CCFF- Crea orden de compra.");
                CrossplayUtils.sendMessage(player, "&#00f5ff/bazar claim &#E6CCFF- Reclama tus entregas del buzón.");
                CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            }
            case "claim" -> {
                CrossplayUtils.sendMessage(player, "&#FFAA00[⏳] Revisando buzón de entregas del Bazar...");
                bazaarManager.reclamarBuzon(player);
            }
            case "sell" -> {
                if (args.length < 2) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /bazar sell <precio_por_unidad>");
                    return true;
                }
                ItemStack itemHand = player.getInventory().getItemInMainHand();
                if (itemHand == null || itemHand.getType() == Material.AIR) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Debes tener un ítem válido en tu mano para venderlo.");
                    return true;
                }
                try {
                    BigDecimal precioUnidad = new BigDecimal(args[1]);
                    if (precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                        CrossplayUtils.sendMessage(player, "&#FF5555[!] El precio debe ser mayor a 0.");
                        return true;
                    }
                    bazaarManager.crearOrdenVenta(player, itemHand.getType().name(), itemHand.getAmount(), precioUnidad);
                } catch (Exception e) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Precio inválido. Usa solo números decimales (Ej: 15.50).");
                }
            }
            case "buy" -> {
                if (args.length < 4) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /bazar buy <item> <cantidad> <precio_por_unidad>");
                    return true;
                }
                try {
                    String itemId = args[1].toUpperCase();
                    int cantidad = Integer.parseInt(args[2]);
                    BigDecimal precioUnidad = new BigDecimal(args[3]);

                    if (cantidad <= 0 || precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                        CrossplayUtils.sendMessage(player, "&#FF5555[!] La cantidad y el precio deben ser mayores a 0.");
                        return true;
                    }
                    bazaarManager.crearOrdenCompra(player, itemId, cantidad, precioUnidad);
                } catch (Exception e) {
                    CrossplayUtils.sendMessage(player, "&#FF5555[!] Datos inválidos. Revisa el nombre del ítem y que los números sean correctos.");
                }
            }
            default -> {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Comando desconocido. Usa &#00f5ff/bazar help &#FF5555para ver la lista de comandos.");
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
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        String subCmd = args[0].toLowerCase();

        if (args.length == 2) {
            if (subCmd.equals("sell")) return SUGGESTED_PRICES;
            if (subCmd.equals("buy")) return List.of("DIAMOND", "IRON_INGOT", "GOLD_INGOT", "COBBLESTONE"); // Sugerencias rápidas
        }

        if (args.length == 3 && subCmd.equals("buy")) {
            return SUGGESTED_AMOUNTS;
        }

        if (args.length == 4 && subCmd.equals("buy")) {
            return SUGGESTED_PRICES;
        }

        return Collections.emptyList();
    }
}
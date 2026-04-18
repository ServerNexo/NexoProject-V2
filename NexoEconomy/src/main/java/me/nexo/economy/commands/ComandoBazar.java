package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.bazar.BazaarManager;
import me.nexo.economy.bazar.BazaarMenu;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

/**
 * 💰 NexoEconomy - Comando del Bazar (Arquitectura Enterprise)
 */
@Singleton
public class ComandoBazar implements CommandExecutor {

    private final NexoEconomy plugin;
    private final BazaarManager bazaarManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoBazar(NexoEconomy plugin, BazaarManager bazaarManager) {
        this.plugin = plugin;
        this.bazaarManager = bazaarManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

        // 🌟 FIX: Menú de ayuda directo y optimizado
        if (subCommand.equals("help")) {
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            CrossplayUtils.sendMessage(player, "&#FFAA00⚖ <bold>COMANDOS DEL BAZAR</bold>");
            CrossplayUtils.sendMessage(player, "&#00f5ff/bazar &#E6CCFF- Abre el menú interactivo.");
            CrossplayUtils.sendMessage(player, "&#00f5ff/bazar sell <precio_ud> &#E6CCFF- Vende el ítem de tu mano.");
            CrossplayUtils.sendMessage(player, "&#00f5ff/bazar buy <item> <cant> <precio_ud> &#E6CCFF- Crea orden de compra.");
            CrossplayUtils.sendMessage(player, "&#00f5ff/bazar claim &#E6CCFF- Reclama tus entregas del buzón.");
            CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
            return true;
        }

        if (subCommand.equals("claim")) {
            CrossplayUtils.sendMessage(player, "&#FFAA00[⏳] Revisando buzón de entregas del Bazar...");
            bazaarManager.reclamarBuzon(player);
            return true;
        }

        if (subCommand.equals("sell")) {
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
            return true;
        }

        if (subCommand.equals("buy")) {
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
            return true;
        }

        // Si pone cualquier otra cosa, le mostramos la ayuda
        CrossplayUtils.sendMessage(player, "&#FF5555[!] Comando desconocido. Usa &#00f5ff/bazar help &#FF5555para ver la lista de comandos.");
        return true;
    }
}
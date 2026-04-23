package me.nexo.economy.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.economy.bazar.BazaarManager;
import me.nexo.economy.bazar.BazaarMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 💰 NexoEconomy - Comando del Bazar (Arquitectura Enterprise Java 21)
 * Rendimiento: Inyección Nativa en CommandMap, Cero Estáticos y Prevención de Ítems Fantasma.
 */
@Singleton
public class ComandoBazar extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final BazaarManager bazaarManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ComandoBazar(BazaarManager bazaarManager, CrossplayUtils crossplayUtils) {
        super("bazar");
        this.description = "Abre y gestiona el Bazar Global.";
        this.aliases = List.of("bazaar", "mercado");
        
        this.bazaarManager = bazaarManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] La consola no puede operar en el Bazar.");
            return true;
        }

        // Abre el menú si no hay argumentos
        if (args.length == 0) {
            // 🌟 INYECCIÓN TRANSITIVA: Pasamos las dependencias necesarias al menú en lugar del 'plugin'
            new BazaarMenu(player, bazaarManager, crossplayUtils).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 🌟 FIX: Menú de ayuda directo y optimizado
        if (subCommand.equals("help")) {
            crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            crossplayUtils.sendMessage(player, "&#FFAA00⚖ <bold>COMANDOS DEL BAZAR</bold>");
            crossplayUtils.sendMessage(player, "&#00f5ff/bazar &#E6CCFF- Abre el menú interactivo.");
            crossplayUtils.sendMessage(player, "&#00f5ff/bazar sell <precio_ud> &#E6CCFF- Vende el ítem de tu mano.");
            crossplayUtils.sendMessage(player, "&#00f5ff/bazar buy <item> <cant> <precio_ud> &#E6CCFF- Crea orden de compra.");
            crossplayUtils.sendMessage(player, "&#00f5ff/bazar claim &#E6CCFF- Reclama tus entregas del buzón.");
            crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            return true;
        }

        if (subCommand.equals("claim")) {
            crossplayUtils.sendMessage(player, "&#FFAA00[⏳] Revisando buzón de entregas del Bazar...");
            bazaarManager.reclamarBuzon(player);
            return true;
        }

        if (subCommand.equals("sell")) {
            if (args.length < 2) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /bazar sell <precio_por_unidad>");
                return true;
            }

            var itemHand = player.getInventory().getItemInMainHand();
            
            // 🌟 PAPER 1.21 FIX: isEmpty() nativo para evitar ítems fantasma (AIR o Amount = 0)
            if (itemHand.isEmpty()) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Debes tener un ítem válido en tu mano para venderlo.");
                return true;
            }

            try {
                var precioUnidad = new BigDecimal(args[1]);
                if (precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] El precio debe ser mayor a 0.");
                    return true;
                }
                bazaarManager.crearOrdenVenta(player, itemHand.getType().name(), itemHand.getAmount(), precioUnidad);
                
            } catch (NumberFormatException e) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Precio inválido. Usa solo números decimales (Ej: 15.50).");
            }
            return true;
        }

        if (subCommand.equals("buy")) {
            if (args.length < 4) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /bazar buy <item> <cantidad> <precio_por_unidad>");
                return true;
            }

            try {
                String itemId = args[1].toUpperCase();
                int cantidad = Integer.parseInt(args[2]);
                var precioUnidad = new BigDecimal(args[3]);

                if (cantidad <= 0 || precioUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    crossplayUtils.sendMessage(player, "&#FF5555[!] La cantidad y el precio deben ser mayores a 0.");
                    return true;
                }
                bazaarManager.crearOrdenCompra(player, itemId, cantidad, precioUnidad);
                
            } catch (NumberFormatException e) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Datos inválidos. Revisa el nombre del ítem y que los números sean correctos.");
            }
            return true;
        }

        // Si pone cualquier otra cosa, le mostramos la ayuda
        crossplayUtils.sendMessage(player, "&#FF5555[!] Comando desconocido. Usa &#00f5ff/bazar help &#FF5555para ver la lista de comandos.");
        return true;
    }
}
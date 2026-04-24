package me.nexo.items.accesorios;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 🎒 NexoItems - Comando Principal de Accesorios (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap nativo, Inyección Estricta, Setters Seguros y Cero Dependencias Muertas.
 */
@Singleton
public class ComandoAccesorios extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final AccesoriosManager manager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección Estricta de Dependencias
    @Inject
    public ComandoAccesorios(AccesoriosManager manager, CrossplayUtils crossplayUtils) {
        super("accesorios");

        // 🌟 FIX ERROR ALIASES: Usamos los Setters oficiales para mantener el encapsulamiento
        this.setDescription("Gestiona la bóveda y obtención de accesorios RPG.");
        this.setAliases(List.of("acc", "accessories"));

        this.manager = manager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // 🌟 JAVA 21: Pattern Matching
        if (!(sender instanceof Player player)) {
            // 🌟 FIX: Adiós '§c' legacy.
            sender.sendMessage("[!] Comando solo disponible para jugadores en el plano físico.");
            return true;
        }

        // 1. Abrir Bóveda (/accesorios)
        if (args.length == 0) {
            manager.abrirBolsa(player);
            return true;
        }

        // 2. Ayuda (/accesorios help)
        if (args[0].equalsIgnoreCase("help")) {
            crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            crossplayUtils.sendMessage(player, "&#ff00ff<bold>💍 SISTEMA DE ACCESORIOS</bold>");
            crossplayUtils.sendMessage(player, "&#00f5ff/accesorios &#E6CCFF- Abre tu bóveda de accesorios.");

            if (player.hasPermission("nexo.admin")) {
                crossplayUtils.sendMessage(player, "&#00f5ff/accesorios give <jugador> <id> &#E6CCFF- Concede un accesorio.");
            }

            crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            return true;
        }

        // 3. Dar Accesorio (Admin - /accesorios give <jugador> <id>)
        if (args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("nexo.admin")) {
                crossplayUtils.sendMessage(player, "&#FF3366[!] El Vacío rechaza tu petición (Sin Permisos).");
                return true;
            }

            if (args.length < 3) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Uso incorrecto: /accesorios give <jugador> <id>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                crossplayUtils.sendMessage(player, "&#FF3366[!] Ese jugador no se encuentra en este plano.");
                return true;
            }

            String accId = args[2].toLowerCase();
            var item = manager.generarAccesorio(accId);

            // 🌟 GHOST-ITEM PROOF
            if (item == null || item.isEmpty()) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Anomalía detectada: El accesorio '" + accId + "' no existe en el registro.");
                return true;
            }

            target.getInventory().addItem(item);
            crossplayUtils.sendMessage(target, "&#00f5ff[📦] Has recibido un nuevo Accesorio de poder.");
            crossplayUtils.sendMessage(player, "&#00f5ff[📦] Accesorio entregado con éxito a " + target.getName() + ".");
            return true;
        }

        return true;
    }
}
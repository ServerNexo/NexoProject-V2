package me.nexo.items.mochilas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 🎒 NexoItems - Comando Player Vaults (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap nativo, Cero Estáticos y Propagación de Dependencias.
 */
@Singleton
public class ComandoPV extends Command {

    private static final String ERR_USAGE = "&#FF5555[!] Uso: &#FFAA00/pv <número>";
    private static final String ERR_INVALID_NUM = "&#FF5555[!] Error: Debes ingresar un número válido.";
    private static final String ERR_NO_PERM = "&#FF5555[!] Acceso denegado a la bóveda #%num%.";

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final NexoCore corePlugin;
    private final MochilaManager manager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ComandoPV(NexoItems plugin, NexoCore corePlugin, MochilaManager manager, CrossplayUtils crossplayUtils) {
        super("pv");
        this.description = "Abre tu mochila virtual.";
        this.aliases = List.of("playervault", "vault", "mochila");
        
        this.plugin = plugin;
        this.corePlugin = corePlugin;
        this.manager = manager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // Validación con Pattern Matching de Java 21
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal requiere un operario humano.");
            return true;
        }

        // Si escribe solo "/pv", abrimos el Selector
        if (args.length == 0) {
            // 🌟 INYECCIÓN TRANSITIVA: Pasamos las herramientas exactas que el menú necesita
            new PVMenu(player, plugin, corePlugin, crossplayUtils).open();
            return true;
        }

        if (args.length != 1) {
            crossplayUtils.sendMessage(player, ERR_USAGE);
            return true;
        }

        try {
            int vaultNumber = Integer.parseInt(args[0]);

            // Validación de Permisos de negocio
            if (!player.hasPermission("nexo.pv." + vaultNumber) && !player.hasPermission("nexo.pv.*")) {
                crossplayUtils.sendMessage(player, ERR_NO_PERM.replace("%num%", String.valueOf(vaultNumber)));
                return true;
            }

            // Llamada Inyectada y Segura al Manager (Que maneja los Hilos Virtuales)
            manager.abrirMochila(player, vaultNumber);

        } catch (NumberFormatException e) {
            crossplayUtils.sendMessage(player, ERR_INVALID_NUM);
        }

        return true;
    }
}
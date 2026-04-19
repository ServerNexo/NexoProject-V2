package me.nexo.items.mochilas;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.NexoItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🎒 NexoItems - Comando de Mochilas/Bóvedas (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoPV extends Command {

    private final NexoItems plugin;
    private final MochilaManager manager;

    private static final String ERR_USAGE = "&#FF5555[!] Uso: &#FFAA00/pv <número>";
    private static final String ERR_INVALID_NUM = "&#FF5555[!] Error: Debes ingresar un número válido.";
    private static final String ERR_NO_PERM = "&#FF5555[!] Acceso denegado a la bóveda #%num%.";

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoPV(NexoItems plugin, MochilaManager manager) {
        super("pv"); // 🌟 Nombre nativo base
        this.setAliases(List.of("playervault", "mochila", "vault", "mochilas")); // Alias nativos

        this.plugin = plugin;
        this.manager = manager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 PROTECCIÓN DE CONSOLA (Fixeado el NPE oculto)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal no tiene acceso a las dimensiones de bolsillo (Mochilas).");
            return true;
        }

        // Si escribe solo "/pv", abrimos el Selector Visual
        if (args.length == 0) {
            new PVMenu(player, plugin).open();
            return true;
        }

        try {
            int vaultNumber = Integer.parseInt(args[0]);

            // Validación de Permisos Segura
            if (!player.hasPermission("nexo.pv." + vaultNumber) && !player.hasPermission("nexo.pv.*")) {
                CrossplayUtils.sendMessage(player, ERR_NO_PERM.replace("%num%", String.valueOf(vaultNumber)));
                return true;
            }

            // Llamada Inyectada y Segura en O(1)
            manager.abrirMochila(player, vaultNumber);

        } catch (NumberFormatException e) {
            CrossplayUtils.sendMessage(player, ERR_INVALID_NUM);
        }

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        // Sugerimos los primeros 9 números por comodidad
        if (args.length == 1) {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .toList();
        }

        return Collections.emptyList();
    }
}
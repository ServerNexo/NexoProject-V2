package me.nexo.colecciones.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.SlayerMenu;
import me.nexo.colecciones.slayers.ActiveSlayer;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 📚 NexoColecciones - Comando de Cacería Slayer (Arquitectura Enterprise)
 */
@Singleton
public class ComandoSlayer implements CommandExecutor {

    private final NexoColecciones plugin;
    private final SlayerManager slayerManager;

    // 💉 PILAR 3: Inyección de Dependencias Directa
    @Inject
    public ComandoSlayer(NexoColecciones plugin, SlayerManager slayerManager) {
        this.plugin = plugin;
        this.slayerManager = slayerManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        // 💻 PROTECCIÓN DE CONSOLA (Sin excepciones nulas)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal no puede invocar Slayers ni abrir el menú de cacería.");
            return true;
        }

        // ❌ SUBCOMANDO: CANCELAR CONTRATO
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            ActiveSlayer activo = slayerManager.getActiveSlayer(player.getUniqueId());

            if (activo != null) {
                if (activo.getBossBar() != null) activo.getBossBar().removeAll();
                slayerManager.removeActiveSlayer(player.getUniqueId());

                // 🌟 FIX: Mensajes Hexadecimales directos (0% Lag I/O)
                CrossplayUtils.sendMessage(player, "&#FF5555[!] <bold>CONTRATO CANCELADO:</bold> &#E6CCFFHas abandonado la cacería de esta bestia.");
            } else {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] No tienes ningún contrato Slayer activo en este momento.");
            }
            return true;
        }

        // ⚔️ SUBCOMANDO: INICIAR CONTRATO DIRECTO (/slayer <id>)
        if (args.length == 1) {
            slayerManager.iniciarSlayer(player, args[0]);
            return true;
        }

        // 🌟 APERTURA DEL MENÚ PRINCIPAL SLAYER (Arquitectura Omega)
        new SlayerMenu(player, plugin).open();
        return true;
    }
}
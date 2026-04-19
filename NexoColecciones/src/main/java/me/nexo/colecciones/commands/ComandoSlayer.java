package me.nexo.colecciones.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.menu.SlayerMenu;
import me.nexo.colecciones.slayers.ActiveSlayer;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 📚 NexoColecciones - Comando de Cacería Slayer (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoSlayer extends Command {

    private final NexoColecciones plugin;
    private final SlayerManager slayerManager;

    // 💉 PILAR 3: Inyección de Dependencias Directa
    @Inject
    public ComandoSlayer(NexoColecciones plugin, SlayerManager slayerManager) {
        super("slayer"); // 🌟 Nombre nativo base
        this.setAliases(List.of("caceria", "cazar", "slayers")); // Alias nativos

        this.plugin = plugin;
        this.slayerManager = slayerManager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

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

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        // Sugerimos "cancel" como primer argumento rápido
        if (args.length == 1) {
            return List.of("cancel").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
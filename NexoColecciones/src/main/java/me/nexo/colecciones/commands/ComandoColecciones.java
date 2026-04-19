package me.nexo.colecciones.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.colecciones.ColeccionesConfig;
import me.nexo.colecciones.colecciones.CollectionManager;
import me.nexo.colecciones.menu.ColeccionesMenu;
import me.nexo.colecciones.slayers.SlayerManager;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 📚 NexoColecciones - Comando Principal (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoColecciones extends Command {

    private final NexoColecciones plugin;
    private final ColeccionesConfig config;
    private final CollectionManager collectionManager;
    private final SlayerManager slayerManager;

    // 💉 PILAR 3: Inyección de Dependencias Directa
    @Inject
    public ComandoColecciones(NexoColecciones plugin, ColeccionesConfig config, CollectionManager collectionManager, SlayerManager slayerManager) {
        super("colecciones"); // 🌟 Nombre nativo base
        this.setAliases(List.of("col", "collection", "coleccion")); // Alias nativos

        this.plugin = plugin;
        this.config = config;
        this.collectionManager = collectionManager;
        this.slayerManager = slayerManager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 COMPORTAMIENTO DE CONSOLA
        if (!(sender instanceof Player player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                config.recargarConfig();
                collectionManager.cargarDesdeConfig();
                slayerManager.cargarSlayers();
                sender.sendMessage("[NexoColecciones] Archivos recargados exitosamente en la memoria RAM.");
            } else {
                sender.sendMessage("[!] La terminal solo admite el argumento: /colecciones reload");
            }
            return true;
        }

        // 🔄 SUBCOMANDO: RECARGA DE CONFIGURACIÓN
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("nexocolecciones.admin")) {
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Acceso denegado: El motor requiere autorización humana de nivel Administrador.");
                return true;
            }

            config.recargarConfig();
            collectionManager.cargarDesdeConfig();
            slayerManager.cargarSlayers();

            CrossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>SISTEMA REINICIADO:</bold> &#E6CCFFCategorías, Tiers y contratos Slayer recargados en RAM.");
            return true;
        }

        // 🏆 SUBCOMANDO: TOPS ASÍNCRONOS
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            collectionManager.calcularTopAsync(player, args[1]);
            return true;
        }

        // 🌟 APERTURA DE MENÚ PRINCIPAL
        new ColeccionesMenu(player, plugin, ColeccionesMenu.MenuType.MAIN, "", "").open();
        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            sub.add("top");
            if (sender.hasPermission("nexocolecciones.admin")) {
                sub.add("reload");
            }
            return sub.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            // Sugerimos categorías comunes para el top
            return List.of("MINERIA", "COMBATE", "AGRICULTURA").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
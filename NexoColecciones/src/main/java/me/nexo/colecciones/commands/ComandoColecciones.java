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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 📚 NexoColecciones - Comando Principal y Autocompletado (Arquitectura Enterprise)
 * Rendimiento: Registro Nativo (PaperMC), TabCompleter Integrado y Dependencias Propagadas.
 */
@Singleton
public class ComandoColecciones extends Command {

    private final NexoColecciones plugin;
    private final ColeccionesConfig config;
    private final CollectionManager collectionManager;
    private final SlayerManager slayerManager;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ComandoColecciones(NexoColecciones plugin, ColeccionesConfig config, 
                              CollectionManager collectionManager, SlayerManager slayerManager,
                              CrossplayUtils crossplayUtils) {
        super("colecciones"); // Nombre del comando base nativo
        this.setDescription("Abre el menú interactivo de colecciones.");
        this.setUsage("/colecciones [reload|top]");
        this.setAliases(List.of("col", "coleccion")); // Alias configurados desde código
        
        this.plugin = plugin;
        this.config = config;
        this.collectionManager = collectionManager;
        this.slayerManager = slayerManager;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 FIX: execute() nativo en lugar de onCommand()
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 COMPORTAMIENTO DE CONSOLA (Protección y texto plano)
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
                // 🌟 FIX: Mensajes directos con instancia inyectada para Cero Lag I/O
                crossplayUtils.sendMessage(player, "&#FF5555[!] Acceso denegado: El motor requiere autorización humana de nivel Administrador.");
                return true;
            }

            // Recargamos usando los managers inyectados limpiamente
            config.recargarConfig();
            collectionManager.cargarDesdeConfig();
            slayerManager.cargarSlayers();

            crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>SISTEMA REINICIADO:</bold> &#E6CCFFCategorías, Tiers y contratos Slayer recargados en RAM.");
            return true;
        }

        // 🏆 SUBCOMANDO: TOPS ASÍNCRONOS
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            collectionManager.calcularTopAsync(player, args[1]);
            return true;
        }

        // 🌟 APERTURA DE MENÚ PRINCIPAL
        // Propagamos las herramientas inyectadas a la GUI
        new ColeccionesMenu(player, plugin, collectionManager, crossplayUtils, ColeccionesMenu.MenuType.MAIN, "", "").open();
        return true;
    }

    // 🌟 FIX: TabCompleter fusionado en la misma clase
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // Autocompletado dinámico basado en permisos
            Stream<String> subCommands = sender.hasPermission("nexocolecciones.admin")
                    ? Stream.of("reload", "top")
                    : Stream.of("top");

            return subCommands
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList(); // .toList() nativo de Java 16+
        }

        // Retornamos lista inmutable para no crear basura en RAM
        return Collections.emptyList();
    }
}
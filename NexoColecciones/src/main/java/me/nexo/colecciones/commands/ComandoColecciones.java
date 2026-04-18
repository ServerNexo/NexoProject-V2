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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * 📚 NexoColecciones - Comando Principal y Subcomandos (Arquitectura Enterprise)
 */
@Singleton
public class ComandoColecciones implements CommandExecutor {

    private final NexoColecciones plugin;
    private final ColeccionesConfig config;
    private final CollectionManager collectionManager;
    private final SlayerManager slayerManager;

    // 💉 PILAR 3: Inyección de Dependencias Directa
    @Inject
    public ComandoColecciones(NexoColecciones plugin, ColeccionesConfig config, CollectionManager collectionManager, SlayerManager slayerManager) {
        this.plugin = plugin;
        this.config = config;
        this.collectionManager = collectionManager;
        this.slayerManager = slayerManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

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
                // 🌟 FIX: Mensajes directos para Cero Lag I/O
                CrossplayUtils.sendMessage(player, "&#FF5555[!] Acceso denegado: El motor requiere autorización humana de nivel Administrador.");
                return true;
            }

            // 🌟 Recargamos usando los managers inyectados limpiamente
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
}
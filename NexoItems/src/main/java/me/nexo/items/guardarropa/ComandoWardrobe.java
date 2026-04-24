package me.nexo.items.guardarropa;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 🎒 NexoItems - Comando Principal del Guardarropa (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap nativo, Setters Seguros, Cero Dependencias Muertas e Inyección Estricta.
 */
@Singleton
public class ComandoWardrobe extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final GuardarropaListener listener;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Directa (Eliminado 'plugin' porque no se usa)
    @Inject
    public ComandoWardrobe(GuardarropaListener listener, CrossplayUtils crossplayUtils) {
        super("wardrobe");

        // 🌟 FIX ERROR ENCAPSULAMIENTO: Usamos los Setters oficiales de la API
        this.setDescription("Abre el menú de Guardarropa RPG.");
        this.setAliases(List.of("armario"));
        this.setPermission("nexoitems.user");
        this.setPermissionMessage("No tienes permiso para usar este comando.");

        this.listener = listener;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // 🌟 JAVA 21: Pattern Matching limpio
        if (!(sender instanceof Player player)) {
            // 🌟 FIX: Adiós al '§c' legacy. La consola usa texto puro y directo.
            sender.sendMessage("[!] El guardarropa solo está disponible para jugadores en el plano físico.");
            return true;
        }

        // 1. Abrir Menú (/wardrobe o /armario)
        if (args.length == 0) {
            listener.abrirMenu(player);
            return true;
        }

        // 2. Ayuda (/wardrobe help)
        if (args[0].equalsIgnoreCase("help")) {
            // 🌟 USO DE DEPENDENCIA INYECTADA (Cero estáticos)
            crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            crossplayUtils.sendMessage(player, "&#ff00ff👔 <bold>SISTEMA DE GUARDARROPA</bold>");
            crossplayUtils.sendMessage(player, "&#00f5ff/wardrobe &#E6CCFF- Abre tu armario de armaduras.");
            crossplayUtils.sendMessage(player, "&#555555--------------------------------");
            return true;
        }

        return true;
    }
}
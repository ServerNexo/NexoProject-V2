package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.utils.NexoColor;
import me.nexo.items.managers.ItemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🎒 NexoItems - Comando de Generación de Ítems (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoTest extends Command {

    private static final String ERR_NOT_PLAYER = "&#FF5555[!] El terminal requiere un operario humano.";
    private static final String ERR_USAGE = "&#FF5555[!] Uso: &#FFAA00/nexoitem <arma|armadura|herramienta> <id>";
    private static final String ERR_NOT_FOUND = "&#FF5555[!] Archivo de datos no encontrado para el ID: &#FFAA00%id%";
    private static final String MSG_SUCCESS = "&#55FF55[✓] Ítem generado e inyectado en el inventario local.";

    @Inject
    public ComandoTest() {
        super("nexoitem"); // 🌟 Nombre nativo base
        this.setAliases(List.of("nitem", "testitem")); // Alias extra
        this.setPermission("nexoitems.admin");
        this.setPermissionMessage("§c[!] El Vacío rechaza tu petición (Sin Permisos).");
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(NexoColor.parse(ERR_NOT_PLAYER));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(NexoColor.parse(ERR_USAGE));
            return true;
        }

        String type = args[0].toLowerCase();
        String id = args[1];
        ItemStack item = null;

        // 🌟 Switch de Java 21 para mayor velocidad
        switch (type) {
            case "arma" -> item = ItemManager.generarArmaRPG(id);
            case "armadura" -> item = ItemManager.generarArmadura(id);
            case "herramienta" -> item = ItemManager.generarHerramienta(id);
        }

        if (item == null) {
            player.sendMessage(NexoColor.parse(ERR_NOT_FOUND.replace("%id%", id)));
            return true;
        }

        player.getInventory().addItem(item);
        player.sendMessage(NexoColor.parse(MSG_SUCCESS));
        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // Ocultar autocompletado si no es admin
        if (!sender.hasPermission("nexoitems.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return List.of("arma", "armadura", "herramienta").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // Aquí podríamos autocompletar los IDs de los items leyendo los YAML,
        // pero por ahora devolvemos vacío para el segundo argumento.
        if (args.length == 2) {
            return List.of("<id_del_item>");
        }

        return Collections.emptyList();
    }
}
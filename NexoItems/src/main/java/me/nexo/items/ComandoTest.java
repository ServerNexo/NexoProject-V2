package me.nexo.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.items.managers.ItemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 🎒 NexoItems - Comando de Pruebas de Desarrollo (Arquitectura Enterprise Java 21)
 * Rendimiento: Switch Expressions nativas, CommandMap nativo y Cero Estáticos.
 */
@Singleton
public class ComandoTest extends Command {

    private static final String ERR_NOT_PLAYER = "&#FF5555[!] El terminal requiere un operario humano.";
    private static final String ERR_USAGE = "&#FF5555[!] Uso: &#FFAA00/nexoitem <arma|armadura|herramienta> <id>";
    private static final String ERR_NOT_FOUND = "&#FF5555[!] Archivo de datos no encontrado para el ID: &#FFAA00%id%";
    private static final String MSG_SUCCESS = "&#55FF55[✓] Ítem generado e inyectado en el inventario local.";

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final ItemManager itemManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public ComandoTest(ItemManager itemManager, CrossplayUtils crossplayUtils) {
        super("test");
        this.description = "Comando de desarrollador para probar items y menús.";
        this.aliases = List.of("nexoitem", "testitem");
        
        this.itemManager = itemManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // Validación con Pattern Matching de Java 21
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal requiere un operario humano.");
            return true;
        }

        if (args.length < 2) {
            crossplayUtils.sendMessage(player, ERR_USAGE);
            return true;
        }

        String type = args[0].toLowerCase();
        String id = args[1];

        // 🌟 JAVA 21: Switch Expression de alto rendimiento (Reemplaza los if-else encadenados)
        ItemStack item = switch (type) {
            case "arma" -> itemManager.generarArmaRPG(id);
            case "armadura" -> itemManager.generarArmadura(id);
            case "herramienta" -> itemManager.generarHerramienta(id);
            default -> null;
        };

        if (item == null) {
            crossplayUtils.sendMessage(player, ERR_NOT_FOUND.replace("%id%", id));
            return true;
        }

        player.getInventory().addItem(item);
        crossplayUtils.sendMessage(player, MSG_SUCCESS);
        return true;
    }
}
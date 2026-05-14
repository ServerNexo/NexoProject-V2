package me.aeroxis.factories.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.AeroxisCore;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.factories.core.StructureTemplate;
import me.aeroxis.factories.managers.BlueprintManager;
import me.aeroxis.factories.managers.BlueprintScanner; // 🌟 IMPORTAMOS EL ESCÁNER
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 🏭 AeroxisFactories - Comando Principal y Autocompletado (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap nativo, Setters Seguros e Inyección Estricta de Dependencias.
 */
@Singleton
public class ComandoFactory extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final AeroxisCore core;
    private final BlueprintManager blueprintManager;
    private final BlueprintScanner blueprintScanner; // 🌟 NUEVA DEPENDENCIA
    private final CrossplayUtils crossplayUtils;

    // 🌟 OPTIMIZACIÓN: Colección inmutable nativa para el autocompletado
    private final List<String> subCommands = List.of("test", "cancel", "scan"); // 🌟 AÑADIDO 'SCAN'

    // 💉 PILAR 1: Inyección Estricta (Cero llamadas estáticas a getPlugin)
    @Inject
    public ComandoFactory(AeroxisCore core, BlueprintManager blueprintManager, BlueprintScanner blueprintScanner, CrossplayUtils crossplayUtils) {
        super("fabrica");

        this.setDescription("Herramienta principal de administración de fábricas del Nexo.");
        this.setAliases(List.of("factory", "factories"));

        this.core = core;
        this.blueprintManager = blueprintManager;
        this.blueprintScanner = blueprintScanner; // 🌟 INYECTADO
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 FIX: Suprimimos la advertencia del puente legacy del ConfigManager
    @SuppressWarnings("deprecation")
    private String getMessage(String path) {
        return core.getConfigManager().getMessage("factories_messages.yml", path);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // 🌟 JAVA 21: Pattern Matching
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getMessage("comandos.factory.no-jugador"));
            return true;
        }

        // ==========================================
        // 📐 NUEVO COMANDO: ESCÁNER ESPACIAL
        // ==========================================
        if (args.length > 0 && args[0].equalsIgnoreCase("scan")) {
            if (!player.hasPermission("nexofactories.admin")) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Acceso denegado. Se requieren privilegios de Ingeniero Jefe.");
                return true;
            }

            if (args.length < 2) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Uso correcto: /fabrica scan <NombreDeTuFabrica>");
                return true;
            }

            // Obtenemos el bloque exacto al que el jugador está mirando (Máx 5 bloques de distancia)
            Block targetBlock = player.getTargetBlockExact(5);

            if (targetBlock == null || targetBlock.getType().isAir()) {
                crossplayUtils.sendMessage(player, "&#FF5555[!] Debes mirar directamente al Bloque Núcleo (Motor) de tu fábrica.");
                return true;
            }

            String factoryType = args[1].toUpperCase();

            // 🌟 DISPARAMOS EL MOTOR DE ESCANEO
            blueprintScanner.createBlueprintItem(player, targetBlock, factoryType);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            var forjaT1 = new StructureTemplate("FORJA_T1");
            forjaT1.addBlock(0, -1, 0, Material.IRON_BLOCK);
            forjaT1.addBlock(0, 1, 0, Material.FURNACE);

            // 🌟 USO DE DEPENDENCIA INYECTADA DIRECTA
            blueprintManager.projectBlueprint(player, player.getLocation().getBlock().getLocation(), forjaT1);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            blueprintManager.clearBlueprint(player);
            crossplayUtils.sendMessage(player, getMessage("comandos.factory.cancelar"));
            return true;
        }

        crossplayUtils.sendMessage(player, getMessage("comandos.factory.ayuda-test"));
        crossplayUtils.sendMessage(player, getMessage("comandos.factory.ayuda-cancelar"));
        crossplayUtils.sendMessage(player, "&#E6CCFF/fabrica scan <Tipo> &#555555- &#FFAA00Escanea el área y crea un plano.");
        return true;
    }

    // ==========================================
    // ⌨️ TAB COMPLETER NATIVO FUSIONADO
    // ==========================================
    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // 🌟 JAVA 21: .toList() nativo (Más rápido que Collectors.toList())
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        // 🌟 OPTIMIZACIÓN: Evita instanciar 'new ArrayList<>()' basura en la memoria RAM
        return List.of();
    }
}
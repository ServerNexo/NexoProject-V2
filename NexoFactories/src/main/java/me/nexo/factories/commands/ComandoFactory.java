package me.nexo.factories.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.core.StructureTemplate;
import me.nexo.factories.managers.BlueprintManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 🏭 NexoFactories - Comando Principal y Autocompletado (Arquitectura Enterprise Java 21)
 * Rendimiento: CommandMap nativo, TabCompleter unificado e Inyección Estricta de Dependencias.
 */
@Singleton
public class ComandoFactory extends Command {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoCore core;
    private final BlueprintManager blueprintManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 OPTIMIZACIÓN: Colección inmutable nativa para el autocompletado
    private final List<String> subCommands = List.of("test", "cancel");

    // 💉 PILAR 1: Inyección Estricta (Cero llamadas estáticas a getPlugin)
    @Inject
    public ComandoFactory(NexoCore core, BlueprintManager blueprintManager, CrossplayUtils crossplayUtils) {
        super("factory");
        this.description = "Herramienta principal de administración de fábricas del Nexo.";
        this.aliases = List.of("factories");

        this.core = core;
        this.blueprintManager = blueprintManager;
        this.crossplayUtils = crossplayUtils;
    }

    private String getMessage(String path) {
        // Obtenemos el mensaje usando la instancia del Core inyectada
        return core.getConfigManager().getMessage("factories_messages.yml", path);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // 🌟 JAVA 21: Pattern Matching
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getMessage("comandos.factory.no-jugador"));
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
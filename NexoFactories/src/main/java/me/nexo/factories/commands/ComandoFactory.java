package me.nexo.factories.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.core.StructureTemplate;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🏭 NexoFactories - Comando Principal (Arquitectura NATIVA)
 * Fusión de Ejecución y Autocompletado, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoFactory extends Command {

    private final NexoFactories plugin;
    private final NexoCore core;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoFactory(NexoFactories plugin) {
        super("factory"); // 🌟 Nombre nativo base
        this.setAliases(List.of("fabrica", "factories")); // Alias nativos

        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class); // Lazy loading seguro
    }

    private String getMessage(String path) {
        return core.getConfigManager().getMessage("factories_messages.yml", path);
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 PROTECCIÓN DE CONSOLA (Fixeado el NPE oculto de CrossplayUtils)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal no tiene acceso a los planos industriales.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            StructureTemplate forjaT1 = new StructureTemplate("FORJA_T1");
            forjaT1.addBlock(0, -1, 0, Material.IRON_BLOCK);
            forjaT1.addBlock(0, 1, 0, Material.FURNACE);
            plugin.getBlueprintManager().projectBlueprint(player, player.getLocation().getBlock().getLocation(), forjaT1);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            plugin.getBlueprintManager().clearBlueprint(player);
            CrossplayUtils.sendMessage(player, getMessage("comandos.factory.cancelar"));
            return true;
        }

        CrossplayUtils.sendMessage(player, getMessage("comandos.factory.ayuda-test"));
        CrossplayUtils.sendMessage(player, getMessage("comandos.factory.ayuda-cancelar"));
        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {

        if (args.length == 1) {
            return List.of("test", "cancel").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
package me.nexo.mechanics.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.skills.SkillTreeMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * ⚙️ NexoMechanics - Comando del Árbol de Habilidades (Arquitectura NATIVA)
 * Fusión de Ejecución directa, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoSkillTree extends Command {

    private final NexoMechanics plugin;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoSkillTree(NexoMechanics plugin) {
        super("skills"); // 🌟 Nombre nativo base
        this.setAliases(List.of("habilidades", "skill", "skilltree")); // Alias nativos
        this.plugin = plugin;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 PROTECCIÓN DE CONSOLA
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] Solo los jugadores pueden abrir el árbol de habilidades.");
            return true;
        }

        // 🌟 Abrir Menú Principal de Skills
        new SkillTreeMenu(player, plugin).open();
        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // No requiere argumentos
        return Collections.emptyList();
    }
}
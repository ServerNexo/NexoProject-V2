package me.nexo.items.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import me.nexo.items.config.ConfigManager;
import me.nexo.items.estaciones.UpgradeMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🎒 NexoItems - Comando de Forja/Upgrade (Arquitectura NATIVA)
 * Fusión de Ejecución directa, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoUpgrade extends Command {

    private final NexoItems plugin;
    private final ConfigManager configManager;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoUpgrade(NexoItems plugin, ConfigManager configManager) {
        super("forja"); // 🌟 Nombre nativo base
        this.setAliases(List.of("upgrade", "upgradeitem")); // Alias nativos

        this.plugin = plugin;
        this.configManager = configManager;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {

        // 💻 PROTECCIÓN DE CONSOLA
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[!] El terminal no puede acceder a la forja.");
            return true;
        }

        // Invocamos el menú inyectando la instancia del plugin
        new UpgradeMenu(player, plugin).open();

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);

        return true;
    }

    // ==========================================
    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO DIRECTO
    // ==========================================
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // Este comando no requiere argumentos
        return Collections.emptyList();
    }
}
package me.nexo.pvp.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.user.UserManager;
import me.nexo.core.user.UserRepository;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.ConfigManager;
import me.nexo.pvp.menus.BlessingMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 🏛️ NexoPvP - Comando Templo (Arquitectura NATIVA)
 * Actúa como una Fábrica: Inyecta herramientas y las pasa al menú.
 * Fusión de Ejecución directa, bypassing estricto de PaperMC.
 */
@Singleton
public class ComandoTemplo extends Command {

    private final ConfigManager configManager;
    private final UserRepository userRepository;
    private final NexoPvP plugin;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ComandoTemplo(ConfigManager configManager, UserRepository userRepository, NexoPvP plugin) {
        super("templo"); // 🌟 Nombre nativo base
        this.setAliases(List.of("bendicion", "blessing")); // Alias nativos
        this.setPermission("nexopvp.templo");
        this.setPermissionMessage("§c❌ No tienes autorización táctica para este comando.");

        this.configManager = configManager;
        this.userRepository = userRepository;
        this.plugin = plugin;
    }

    // ==========================================
    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    // ==========================================
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[!] El Templo de Bendiciones solo puede ser visitado por almas físicas.");
            return true;
        }

        // 🌟 FIX: Lazy Loading Seguro. Lo llamamos cuando el jugador ejecuta el comando, no en el arranque.
        UserManager userManager = NexoCore.getPlugin(NexoCore.class).getUserManager();

        // 🚀 Le pasamos las herramientas limpias al menú
        new BlessingMenu(player, configManager, userManager, userRepository, plugin).open();
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 2.0f);

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
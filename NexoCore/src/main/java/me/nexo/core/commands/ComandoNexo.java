package me.nexo.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton; // 🌟 IMPORTANTE: No olvides esta importación
import me.nexo.core.config.ConfigManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🏛️ Nexo Network - Comando Principal (Arquitectura NATIVA PaperMC)
 * Bypassea la restricción de paper-plugin.yml extendiendo de la clase abstracta nativa Command.
 * Fusión de Ejecución + TabCompleter con Inyección Guice pura.
 */
@Singleton // 🌟 FIX ENTERPRISE: Esto previene fugas de memoria al autocompletar con TAB. ¡ES VITAL!
public class ComandoNexo extends Command {

    // 💉 PILAR 3: Inyección de Dependencias
    private final UserManager userManager;
    private final ConfigManager configManager;

    private static final List<String> SUB_COMMANDS = Arrays.asList("darxp", "darcombatexp");
    private static final List<String> AMOUNTS = Arrays.asList("100", "500", "1000");

    @Inject
    public ComandoNexo(UserManager userManager, ConfigManager configManager) {
        super("nexocore"); // Nombre principal del comando
        this.setAliases(Arrays.asList("nexo")); // Alias nativo
        this.setPermission("nexo.admin"); // Permiso nativo
        this.setPermissionMessage("§cNo tienes permiso para usar este comando.");

        this.userManager = userManager;
        this.configManager = configManager;
    }

    // ⚙️ MOTOR DE EJECUCIÓN NATIVO
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length < 3) {
            enviarMensaje(sender, "§cUso: /" + commandLabel + " <darxp|darcombatexp> <jugador> <cantidad>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        Player objetivo = Bukkit.getPlayer(args[1]);

        if (objetivo == null || !objetivo.isOnline()) {
            enviarMensaje(sender, configManager.getMessages().comandos().nexocore().errores().cargando());
            return true;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            enviarMensaje(sender, "§cLa cantidad debe ser un número entero válido.");
            return true;
        }

        // 🔀 ENRUTADOR DE SUBCOMANDOS
        switch (subCommand) {
            case "darxp":
                darXp(sender, objetivo, cantidad);
                break;
            case "darcombatexp":
                darCombateXp(sender, objetivo, cantidad);
                break;
            default:
                enviarMensaje(sender, "§cSubcomando desconocido. Usa darxp o darcombatexp.");
                break;
        }

        return true;
    }

    // 🧠 MOTOR DE AUTOCOMPLETADO NATIVO (Zero-Lag)
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (!sender.hasPermission("nexo.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            return AMOUNTS.stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    // ========================================================================
    // 💡 LÓGICA DE NEGOCIO INTACTA (Type-Safe & Crossplay)
    // ========================================================================

    private void darXp(CommandSender sender, Player objetivo, int cantidad) {
        NexoUser user = userManager.getUserOrNull(objetivo.getUniqueId());
        if (user == null) {
            enviarMensaje(sender, configManager.getMessages().comandos().nexocore().errores().cargando());
            return;
        }

        int nivelActual = user.getNexoNivel();
        int xpActual = user.getNexoXp() + cantidad;

        while (xpActual >= (nivelActual * 100)) {
            xpActual -= (nivelActual * 100);
            nivelActual++;
            CrossplayUtils.sendTitle(objetivo,
                    configManager.getMessages().comandos().nexocore().subidaNivel().nexo().titulo().replace("%level%", String.valueOf(nivelActual)),
                    configManager.getMessages().comandos().nexocore().subidaNivel().nexo().subtitulo()
            );
        }

        user.setNexoNivel(nivelActual);
        user.setNexoXp(xpActual);

        enviarMensaje(sender, configManager.getMessages().comandos().nexocore().exito().darXp()
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%target%", objetivo.getName()));
    }

    private void darCombateXp(CommandSender sender, Player objetivo, int cantidad) {
        NexoUser user = userManager.getUserOrNull(objetivo.getUniqueId());
        if (user == null) {
            enviarMensaje(sender, configManager.getMessages().comandos().nexocore().errores().cargando());
            return;
        }

        int nivelActual = user.getCombateNivel();
        int xpActual = user.getCombateXp() + cantidad;

        while (xpActual >= (nivelActual * 100)) {
            xpActual -= (nivelActual * 100);
            nivelActual++;
            CrossplayUtils.sendTitle(objetivo,
                    configManager.getMessages().comandos().nexocore().subidaNivel().combate().titulo().replace("%level%", String.valueOf(nivelActual)),
                    configManager.getMessages().comandos().nexocore().subidaNivel().combate().subtitulo()
            );
        }

        user.setCombateNivel(nivelActual);
        user.setCombateXp(xpActual);

        CrossplayUtils.sendMessage(objetivo, configManager.getMessages().comandos().nexocore().feedback().recibirCombateXp()
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%xp%", String.valueOf(xpActual))
                .replace("%xpreq%", String.valueOf(nivelActual * 100)));

        enviarMensaje(sender, configManager.getMessages().comandos().nexocore().exito().darCombateXp()
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%target%", objetivo.getName()));
    }

    // 📱 PILAR 6: Conciencia Cross-Play
    private void enviarMensaje(CommandSender sender, String mensaje) {
        if (sender instanceof Player player) {
            CrossplayUtils.sendMessage(player, mensaje);
        } else {
            sender.sendMessage(mensaje);
        }
    }
}
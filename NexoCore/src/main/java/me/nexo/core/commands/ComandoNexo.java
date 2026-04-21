package me.nexo.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ Nexo Network - Comando Principal (Arquitectura Enterprise / Lamp Framework)
 * Cero 'args.length', cero TabCompleters manuales, inyección limpia y TYPE-SAFE CONFIGS.
 */
@Singleton // 🌟 FIX CRÍTICO: Una sola instancia manejada por Guice y Lamp
@Command({"nexocore", "nexo"})
@CommandPermission("nexo.admin")
public class ComandoNexo {

    private final UserManager userManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public ComandoNexo(UserManager userManager, ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.userManager = userManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    @Subcommand("darxp")
    public void darXp(CommandSender sender, Player objetivo, int cantidad) {
        // 💡 PILAR 1: Lamp ya valida si "cantidad" es un número y si el jugador está online.
        NexoUser user = userManager.getUserOrNull(objetivo.getUniqueId());

        if (user == null) {
            // 🛡️ PILAR 2: TYPE-SAFE CONFIGS (Mapeo directo a Objetos)
            enviarMensaje(sender, configManager.getMessages().comandos().nexocore().errores().cargando());
            return;
        }

        int nivelActual = user.getNexoNivel();
        int xpActual = user.getNexoXp() + cantidad;

        while (xpActual >= (nivelActual * 100)) {
            xpActual -= (nivelActual * 100);
            nivelActual++;
            
            // 🌟 Usamos la instancia inyectada de CrossplayUtils
            crossplayUtils.sendTitle(objetivo,
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

    @Subcommand("darcombatexp")
    public void darCombateXp(CommandSender sender, Player objetivo, int cantidad) {
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
            
            // 🌟 Usamos la instancia inyectada
            crossplayUtils.sendTitle(objetivo,
                    configManager.getMessages().comandos().nexocore().subidaNivel().combate().titulo().replace("%level%", String.valueOf(nivelActual)),
                    configManager.getMessages().comandos().nexocore().subidaNivel().combate().subtitulo()
            );
        }

        user.setCombateNivel(nivelActual);
        user.setCombateXp(xpActual);

        crossplayUtils.sendMessage(objetivo, configManager.getMessages().comandos().nexocore().feedback().recibirCombateXp()
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%xp%", String.valueOf(xpActual))
                .replace("%xpreq%", String.valueOf(nivelActual * 100)));

        enviarMensaje(sender, configManager.getMessages().comandos().nexocore().exito().darCombateXp()
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%target%", objetivo.getName()));
    }

    // 📱 PILAR 6: Conciencia Cross-Play y soporte para la Consola usando Java 21
    private void enviarMensaje(CommandSender sender, String mensaje) {
        if (sender instanceof Player player) {
            crossplayUtils.sendMessage(player, mensaje);
        } else {
            // 🌟 FIX: Si es la consola, usamos CrossplayUtils para enviar el Componente Kyori parseado con ANSI
            sender.sendMessage(crossplayUtils.parseCrossplay(null, mensaje));
        }
    }
}
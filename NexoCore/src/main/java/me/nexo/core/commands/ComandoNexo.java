package me.nexo.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.config.ConfigManager;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.menus.CosmeticsMenu;
import me.nexo.core.user.NexoUser;
import me.nexo.core.user.UserManager;
import me.nexo.core.utils.SoundManager; // 🌟 IMPORTAMOS EL MOTOR DE SONIDO
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
public class ComandoNexo {

    private final UserManager userManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;
    private final SoundManager soundManager; // 🌟 NUEVO

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public ComandoNexo(UserManager userManager, ConfigManager configManager, CrossplayUtils crossplayUtils, SoundManager soundManager) {
        this.userManager = userManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        this.soundManager = soundManager;
    }

    // ==========================================
    // 🎨 MENÚ DE COSMÉTICOS (Jugadores)
    // ==========================================
    @Command({"color", "identidad", "estilos"})
    public void openCosmetics(Player player) {
        soundManager.playMenuOpen(player); // 🔊 Feedback Inmersivo
        new CosmeticsMenu(player, crossplayUtils, userManager, soundManager).open();
    }

    // ==========================================
    // ⚙️ COMANDOS DE ADMINISTRADOR
    // ==========================================
    @Subcommand("darxp")
    @CommandPermission("nexo.admin") // 🛡️ Protegido
    public void darXp(CommandSender sender, Player objetivo, int cantidad) {
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
    @CommandPermission("nexo.admin") // 🛡️ Protegido
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

    // ==========================================
    // 🎁 INTEGRACIÓN CON CAJAS (Crates Bridge)
    // Uso en consola: /nexo internal givecosmetic <jugador> <id_cosmetico>
    // ==========================================
    @Subcommand("internal givecosmetic")
    @CommandPermission("nexo.admin") // 🛡️ Protegido
    public void giveCosmetic(CommandSender sender, Player target, String cosmeticId) {
        NexoUser user = userManager.getUserOrNull(target.getUniqueId());

        if (user == null) {
            enviarMensaje(sender, "<red>❌ El usuario no está cargado en la memoria del Core.</red>");
            return;
        }

        if (user.unlockCosmetic(cosmeticId)) {
            userManager.saveUserAsync(user);

            enviarMensaje(sender, "<green>✅ Cosmético '" + cosmeticId + "' otorgado a " + target.getName() + ".</green>");

            target.playSound(target.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            crossplayUtils.sendMessage(target, "<green>🎉 ¡Has obtenido un nuevo Estilo de Chat!</green>");
            crossplayUtils.sendMessage(target, "<yellow>💡 Escribe <aqua>/color</aqua> para equiparlo.</yellow>");
        } else {
            enviarMensaje(sender, "<yellow>⚠️ El jugador ya poseía el cosmético '" + cosmeticId + "'.</yellow>");
        }
    }

    // 📱 PILAR 6: Conciencia Cross-Play y soporte para la Consola
    private void enviarMensaje(CommandSender sender, String mensaje) {
        if (sender instanceof Player player) {
            crossplayUtils.sendMessage(player, mensaje);
        } else {
            sender.sendMessage(crossplayUtils.parseCrossplay(null, mensaje));
        }
    }
}
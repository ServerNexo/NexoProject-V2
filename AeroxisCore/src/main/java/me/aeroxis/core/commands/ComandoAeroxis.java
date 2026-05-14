package me.aeroxis.core.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.config.ConfigManager;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.menus.CosmeticsHubRegistry; // 🌟 NUEVO IMPORT: El Registro Dinámico
import me.aeroxis.core.menus.CosmeticsMenu;
import me.aeroxis.core.user.AeroxisUser;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.core.utils.SoundManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * 🏛️ Aeroxis Network - Comando Principal (Arquitectura Enterprise / Lamp Framework)
 * Cero 'args.length', cero TabCompleters manuales, inyección limpia y TYPE-SAFE CONFIGS.
 */
@Singleton // 🌟 FIX CRÍTICO: Una sola instancia manejada por Guice y Lamp
@Command({"aeroxiscore", "aeroxis"})
public class ComandoAeroxis {

    private final UserManager userManager;
    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;
    private final SoundManager soundManager;
    private final CosmeticsHubRegistry hubRegistry; // 🌟 NUEVO: Inyectamos el Hub

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public ComandoAeroxis(UserManager userManager, ConfigManager configManager,
                          CrossplayUtils crossplayUtils, SoundManager soundManager,
                          CosmeticsHubRegistry hubRegistry) { // 🌟 AÑADIDO AL CONSTRUCTOR
        this.userManager = userManager;
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
        this.soundManager = soundManager;
        this.hubRegistry = hubRegistry;
    }

    // ==========================================
    // 🎨 MENÚ DE COSMÉTICOS (Hub Dinámico)
    // ==========================================
    // 🌟 FIX: Ampliamos los alias para que tenga sentido con el nuevo Armario Global
    @Command({"cosmeticos", "armario", "color", "identidad", "estilos"})
    public void openCosmetics(Player player) {
        soundManager.playMenuOpen(player);

        // 🌟 MAGIA PURA: Ahora solo le pasamos el Registro. ¡El menú se dibujará solo!
        new CosmeticsMenu(player, crossplayUtils, hubRegistry).open();
    }

    // ==========================================
    // ⚙️ COMANDOS DE ADMINISTRADOR
    // ==========================================
    @Subcommand("darxp")
    @CommandPermission("aeroxis.admin")
    public void darXp(CommandSender sender, Player objetivo, int cantidad) {
        AeroxisUser user = userManager.getUserOrNull(objetivo.getUniqueId());

        if (user == null) {
            enviarMensaje(sender, configManager.getMessages().comandos().aeroxiscore().errores().cargando());
            return;
        }

        int nivelActual = user.getAeroxisNivel();
        int xpActual = user.getAeroxisXp() + cantidad;

        while (xpActual >= (nivelActual * 100)) {
            xpActual -= (nivelActual * 100);
            nivelActual++;

            crossplayUtils.sendTitle(objetivo,
                    configManager.getMessages().comandos().aeroxiscore().subidaNivel().aeroxis().titulo().replace("%level%", String.valueOf(nivelActual)),
                    configManager.getMessages().comandos().aeroxiscore().subidaNivel().aeroxis().subtitulo()
            );
        }

        user.setAeroxisNivel(nivelActual);
        user.setAeroxisXp(xpActual);

        enviarMensaje(sender, configManager.getMessages().comandos().aeroxiscore().exito().darXp()
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%target%", objetivo.getName()));
    }

    @Subcommand("darcombatexp")
    @CommandPermission("aeroxis.admin")
    public void darCombateXp(CommandSender sender, Player objetivo, int cantidad) {
        AeroxisUser user = userManager.getUserOrNull(objetivo.getUniqueId());

        if (user == null) {
            enviarMensaje(sender, configManager.getMessages().comandos().aeroxiscore().errores().cargando());
            return;
        }

        int nivelActual = user.getCombateNivel();
        int xpActual = user.getCombateXp() + cantidad;

        while (xpActual >= (nivelActual * 100)) {
            xpActual -= (nivelActual * 100);
            nivelActual++;

            crossplayUtils.sendTitle(objetivo,
                    configManager.getMessages().comandos().aeroxiscore().subidaNivel().combate().titulo().replace("%level%", String.valueOf(nivelActual)),
                    configManager.getMessages().comandos().aeroxiscore().subidaNivel().combate().subtitulo()
            );
        }

        user.setCombateNivel(nivelActual);
        user.setCombateXp(xpActual);

        crossplayUtils.sendMessage(objetivo, configManager.getMessages().comandos().aeroxiscore().feedback().recibirCombateXp()
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%xp%", String.valueOf(xpActual))
                .replace("%xpreq%", String.valueOf(nivelActual * 100)));

        enviarMensaje(sender, configManager.getMessages().comandos().aeroxiscore().exito().darCombateXp()
                .replace("%amount%", String.valueOf(cantidad))
                .replace("%target%", objetivo.getName()));
    }

    // ==========================================
    // 🎁 INTEGRACIÓN CON CAJAS (Crates Bridge)
    // ==========================================
    @Subcommand("internal givecosmetic")
    @CommandPermission("aeroxis.admin")
    public void giveCosmetic(CommandSender sender, Player target, String cosmeticId) {
        AeroxisUser user = userManager.getUserOrNull(target.getUniqueId());

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
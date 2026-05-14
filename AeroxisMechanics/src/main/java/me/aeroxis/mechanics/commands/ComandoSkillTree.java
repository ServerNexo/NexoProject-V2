package me.aeroxis.mechanics.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.user.UserManager;
import me.aeroxis.mechanics.AeroxisMechanics;
import me.aeroxis.mechanics.config.ConfigManager;
import me.aeroxis.mechanics.skills.SkillTreeMenu;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚙️ AeroxisMechanics - Comando Principal (Arquitectura Enterprise)
 * Rendimiento: Propagación de Sinergias (Inyección), Cero Estáticos y Mapas Concurrentes.
 * Nota: Lamp (Revxrsal) inyecta este comando nativamente en el CommandMap.
 */
@Singleton
@Command({"skills", "habilidades", "skilltree"})
public class ComandoSkillTree {

    private final AeroxisMechanics plugin;
    private final ConfigManager configManager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 Almacenamiento Thread-Safe para los permisos otorgados en la sesión actual
    private final Map<UUID, PermissionAttachment> sessionPermissions = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección Directa de Sinergias
    @Inject
    public ComandoSkillTree(AeroxisMechanics plugin, ConfigManager configManager,
                            UserManager userManager, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.userManager = userManager;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 COMANDO: /skills (Abre el menú propagando las dependencias)
    @DefaultFor("~")
    public void openMenu(Player player) {
        new SkillTreeMenu(player, plugin, configManager, userManager, crossplayUtils, sessionPermissions).open();
    }

    // 🌟 COMANDO: /skills reload
    @Subcommand("reload")
    @CommandPermission("nexomechanics.admin")
    public void reload(Player player) {
        configManager.reloadMessages();
        // 🌟 Sinergia inyectada en lugar de llamada estática
        crossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().recargaExitosa());
    }
}
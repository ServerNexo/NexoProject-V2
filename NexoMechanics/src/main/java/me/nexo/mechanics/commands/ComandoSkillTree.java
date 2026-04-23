package me.nexo.mechanics.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.user.UserManager;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.skills.SkillTreeMenu;
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
 * ⚙️ NexoMechanics - Comando Principal (Arquitectura Enterprise)
 * Rendimiento: Propagación de Sinergias (Inyección), Cero Estáticos y Mapas Concurrentes.
 * Nota: Lamp (Revxrsal) inyecta este comando nativamente en el CommandMap.
 */
@Singleton
@Command({"skills", "habilidades", "skilltree"})
public class ComandoSkillTree {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;
    private final UserManager userManager;
    private final CrossplayUtils crossplayUtils;

    // 🌟 Almacenamiento Thread-Safe para los permisos otorgados en la sesión actual
    private final Map<UUID, PermissionAttachment> sessionPermissions = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección Directa de Sinergias
    @Inject
    public ComandoSkillTree(NexoMechanics plugin, ConfigManager configManager, 
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
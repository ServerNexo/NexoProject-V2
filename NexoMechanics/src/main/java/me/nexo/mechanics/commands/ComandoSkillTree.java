package me.nexo.mechanics.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.ConfigManager;
import me.nexo.mechanics.skills.SkillTreeMenu;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * ⚙️ NexoMechanics - Comando Principal (Arquitectura Enterprise)
 */
@Singleton
@Command({"skills", "habilidades", "skilltree"})
public class ComandoSkillTree {

    private final NexoMechanics plugin;
    private final ConfigManager configManager;

    @Inject
    public ComandoSkillTree(NexoMechanics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    // 🌟 COMANDO: /skills (Abre el menú)
    @DefaultFor("~")
    public void openMenu(Player player) {
        new SkillTreeMenu(player, plugin).open();
    }

    // 🌟 COMANDO: /skills reload
    @Subcommand("reload")
    @CommandPermission("nexomechanics.admin")
    public void reload(Player player) {
        configManager.reloadMessages();
        CrossplayUtils.sendMessage(player, configManager.getMessages().mensajes().exito().recargaExitosa());
    }
}
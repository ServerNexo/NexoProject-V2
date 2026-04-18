package me.nexo.mechanics.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.nodes.MechanicsMessagesConfig;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;

import java.io.File;

/**
 * ⚙️ NexoMechanics - Config Manager Purificado (Arquitectura Enterprise)
 */
@Singleton
public class ConfigManager {

    private final NexoMechanics plugin;
    private MechanicsMessagesConfig messages;

    @Inject
    public ConfigManager(NexoMechanics plugin) {
        this.plugin = plugin;
        saveDefaultResource("messages.yml");
        loadConfigurate();
    }

    private void saveDefaultResource(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private void loadConfigurate() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            this.messages = loader.load().get(MechanicsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en NexoMechanics: " + e.getMessage());
        }
    }

    public void reloadMessages() {
        loadConfigurate();
    }

    // 💡 PILAR 2: Acceso Type-Safe
    public MechanicsMessagesConfig getMessages() {
        return messages;
    }
}
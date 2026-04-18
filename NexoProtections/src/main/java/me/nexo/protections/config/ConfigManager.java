package me.nexo.protections.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.nodes.ProtectionsMessagesConfig;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;

import java.io.File;

/**
 * 🛡️ NexoProtections - Config Manager Purificado (Arquitectura Enterprise)
 */
@Singleton
public class ConfigManager {

    private final NexoProtections plugin;
    private ProtectionsMessagesConfig messages;

    @Inject
    public ConfigManager(NexoProtections plugin) {
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
            this.messages = loader.load().get(ProtectionsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en NexoProtections: " + e.getMessage());
        }
    }

    public void reloadMessages() {
        loadConfigurate();
    }

    // 💡 PILAR 2: Acceso Type-Safe a todos los textos
    public ProtectionsMessagesConfig getMessages() {
        return messages;
    }
}
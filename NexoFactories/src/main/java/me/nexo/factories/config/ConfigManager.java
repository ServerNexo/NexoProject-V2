package me.nexo.factories.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.config.nodes.FactoriesMessagesConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;

/**
 * 🏭 NexoFactories - Gestor de Configuración Tipado (Arquitectura Enterprise)
 */
@Singleton
public class ConfigManager {

    private final NexoFactories plugin;
    private FactoriesMessagesConfig messages;
    private YamlConfigurationLoader messagesLoader;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoFactories plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        Path path = file.toPath();
        messagesLoader = YamlConfigurationLoader.builder().path(path).build();

        try {
            CommentedConfigurationNode root = messagesLoader.load();
            messages = root.get(FactoriesMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("Error cargando messages.yml de Factories: " + e.getMessage());
            messages = new FactoriesMessagesConfig(); // Fallback seguro
        }
    }

    // 🌟 Acceso Type-Safe a los textos
    public FactoriesMessagesConfig getMessages() {
        return messages;
    }
}
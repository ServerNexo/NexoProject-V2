package me.nexo.dungeons.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.config.nodes.DungeonsMessagesConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;

/**
 * 🏰 NexoDungeons - Gestor de Configuración Tipado (Arquitectura Enterprise)
 */
@Singleton
public class ConfigManager {

    private final NexoDungeons plugin;
    private DungeonsMessagesConfig messages;
    private YamlConfigurationLoader messagesLoader;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoDungeons plugin) {
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
            messages = root.get(DungeonsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico cargando messages.yml de Dungeons: " + e.getMessage());
            messages = new DungeonsMessagesConfig(); // Fallback seguro
        }
    }

    // 🌟 Acceso Type-Safe a los textos (Adiós al viejo getMessage)
    public DungeonsMessagesConfig getMessages() {
        return messages;
    }
}
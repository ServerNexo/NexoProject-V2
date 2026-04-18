package me.nexo.minions.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.minions.NexoMinions;
import me.nexo.minions.config.nodes.MinionsMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 🤖 NexoMinions - Config Manager Purificado (Arquitectura Enterprise)
 */
@Singleton
public class ConfigManager {

    private final NexoMinions plugin;
    private MinionsMessagesConfig messages;

    @Inject
    public ConfigManager(NexoMinions plugin) {
        this.plugin = plugin;
        saveDefaultResource("messages.yml");
        saveDefaultResource("upgrades.yml");
        saveDefaultResource("tiers.yml");
        loadConfigurate();
    }

    private void saveDefaultResource(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false); // 🌟 Seguro: No borra datos.
        }
    }

    private void loadConfigurate() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            this.messages = loader.load().get(MinionsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en NexoMinions: " + e.getMessage());
            // 🌟 FIX: Fallback seguro. Evita NullPointerExceptions en todo el plugin si el YAML se corrompe.
            this.messages = new MinionsMessagesConfig();
        }
    }

    public void reloadMessages() {
        loadConfigurate();
    }

    // 💡 PILAR 2: Acceso Type-Safe
    public MinionsMessagesConfig getMessages() {
        return messages;
    }
}
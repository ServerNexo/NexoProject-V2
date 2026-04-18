package me.nexo.items.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import me.nexo.items.config.nodes.ItemsMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 🎒 NexoItems - Config Manager Purificado (Arquitectura Enterprise)
 */
@Singleton
public class ConfigManager {

    private final NexoItems plugin;
    private ItemsMessagesConfig messages;

    @Inject
    public ConfigManager(NexoItems plugin) {
        this.plugin = plugin;

        // Generamos todos los archivos del módulo
        saveDefaultResource("messages.yml");
        saveDefaultResource("armas.yml");
        saveDefaultResource("armaduras.yml");
        saveDefaultResource("artefactos.yml");
        saveDefaultResource("herramientas.yml");
        saveDefaultResource("encantamientos.yml");
        saveDefaultResource("reforjas.yml");

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
            this.messages = loader.load().get(ItemsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en NexoItems: " + e.getMessage());
        }
    }

    public void reloadMessages() {
        loadConfigurate();
    }

    // 💡 PILAR 2: Acceso Type-Safe Ultra Rápido
    public ItemsMessagesConfig getMessages() {
        return messages;
    }
}
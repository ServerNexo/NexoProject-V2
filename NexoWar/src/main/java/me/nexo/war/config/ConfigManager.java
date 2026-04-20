package me.nexo.war.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.war.NexoWar;
import me.nexo.war.config.nodes.WarMessagesConfig;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;

import java.io.File;

@Singleton
public class ConfigManager {

    private final NexoWar plugin;
    private WarMessagesConfig messages;

    @Inject
    public ConfigManager(NexoWar plugin) {
        this.plugin = plugin;

        // 🌟 FIX: Eliminada la línea que intentaba guardar el config.yml inexistente.
        // Ahora el compilador ya no crasheará buscando un archivo fantasma.
        saveDefaultResource("messages.yml");

        loadConfigurate();
    }

    private void saveDefaultResource(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    // 💡 Carga los textos desde el YML hacia Java
    private void loadConfigurate() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            this.messages = loader.load().get(WarMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en NexoWar: " + e.getMessage());
        }
    }

    // 🔄 MÉTODO NUEVO: Úsalo cuando hagas un comando de recarga en el futuro
    public void reloadMessages() {
        loadConfigurate();
    }

    public WarMessagesConfig getMessages() {
        return messages;
    }
}
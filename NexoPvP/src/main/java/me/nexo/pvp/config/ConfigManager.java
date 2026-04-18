package me.nexo.pvp.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.nodes.PvPMessagesConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ Nexo Network (PvP Module) - Config Manager Purificado
 */
@Singleton
public class ConfigManager {

    private final NexoPvP plugin;
    private final Map<String, FileConfiguration> legacyConfigs = new ConcurrentHashMap<>();

    // 💡 PILAR 2: La nueva forma Type-Safe de acceder a los mensajes
    private PvPMessagesConfig messages;

    @Inject
    public ConfigManager(NexoPvP plugin) {
        this.plugin = plugin;

        saveDefaultResource("config.yml", false);
        saveDefaultResource("messages.yml", true);

        // 🚀 Iniciamos el motor Type-Safe
        loadConfigurate();

        // 🌉 Mantenemos el puente legacy vivo temporalmente
        getConfig("config.yml");
        getConfig("messages.yml");
    }

    private void loadConfigurate() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            this.messages = loader.load().get(PvPMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en NexoPvP: " + e.getMessage());
        }
    }

    // 💡 Método mágico para acceder a los textos
    public PvPMessagesConfig getMessages() {
        return messages;
    }

    // ==========================================
    // 🌉 PUENTE LEGACY
    // ==========================================
    private void saveDefaultResource(String fileName, boolean replace) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists() || replace) {
            try { plugin.saveResource(fileName, replace); } catch (Exception ignored) {}
        }
    }

    @Deprecated
    public FileConfiguration getConfig(String configName) {
        return legacyConfigs.computeIfAbsent(configName, name -> {
            File configFile = new File(plugin.getDataFolder(), name);
            return YamlConfiguration.loadConfiguration(configFile);
        });
    }

    @Deprecated
    public String getMessage(String path) {
        return getConfig("messages.yml").getString(path, "§cMensaje no encontrado: " + path);
    }
}
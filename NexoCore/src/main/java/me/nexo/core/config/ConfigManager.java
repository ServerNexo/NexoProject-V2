package me.nexo.core.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.config.nodes.MessagesConfig; // 🌟 Tu nueva clase mágica
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ConfigManager {

    private final NexoCore plugin;
    private final Map<String, FileConfiguration> legacyConfigs = new ConcurrentHashMap<>();

    // 💡 PILAR 2: Nuestra Configuración Type-Safe
    private MessagesConfig messages;

    @Inject
    public ConfigManager(NexoCore plugin) {
        this.plugin = plugin;

        // 1. Extraer archivos físicos si no existen
        saveDefaultResource("config.yml", false);
        saveDefaultResource("messages.yml", true);

        // 2. 🚀 INICIAR SPONGE CONFIGURATE
        loadConfigurate();

        // 3. Iniciar el puente viejo (Para NexoPvP, etc.)
        getConfig("config.yml");
        getConfig("messages.yml");
    }

    // ==========================================
    // 🚀 NUEVO MOTOR: SPONGE CONFIGURATE
    // ==========================================

    private void loadConfigurate() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            // ¡MAGIA! Lee el YAML y lo transforma en nuestro objeto Java
            this.messages = loader.load().get(MessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml: " + e.getMessage());
        }
    }

    public MessagesConfig getMessages() {
        return messages;
    }

    // ==========================================
    // 🌉 PUENTE LEGACY Y UTILIDADES
    // ==========================================

    private void saveDefaultResource(String fileName, boolean replace) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists() || replace) {
            try { plugin.saveResource(fileName, replace); }
            catch (IllegalArgumentException ignored) {}
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
        return getMessage("messages.yml", path);
    }

    @Deprecated
    public String getMessage(String configName, String path) {
        return getConfig(configName).getString(path, "§cMensaje no encontrado: " + path);
    }

    public void reloadConfigs() {
        saveDefaultResource("messages.yml", true);
        loadConfigurate(); // Recarga el nuevo motor
        legacyConfigs.clear(); // Limpia la caché vieja para que se vuelva a cargar
        getConfig("config.yml");
        getConfig("messages.yml");
    }
}
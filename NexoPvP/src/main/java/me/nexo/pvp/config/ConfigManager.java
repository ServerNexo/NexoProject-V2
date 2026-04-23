package me.nexo.pvp.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.pvp.NexoPvP;
import me.nexo.pvp.config.nodes.PvPMessagesConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ Nexo Network (PvP Module) - Config Manager Purificado
 * Gestor de configuraciones híbrido (Type-Safe + Legacy Bridge).
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

    /**
     * 🚀 Motor de Carga: Sponge Configurate (Type-Safe)
     */
    private void loadConfigurate() {
        var file = new File(plugin.getDataFolder(), "messages.yml");
        var loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            var node = loader.load();
            this.messages = node.get(PvPMessagesConfig.class);
            
            // 🛡️ Fallback preventivo: Salvaguarda contra YAMLs corruptos o vacíos
            if (this.messages == null) {
                plugin.getLogger().warning("⚠️ El archivo messages.yml de NexoPvP parece estar vacío. Usando valores por defecto.");
                this.messages = new PvPMessagesConfig(); 
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico al cargar messages.yml en NexoPvP: " + e.getMessage());
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
        var file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists() || replace) {
            try { 
                plugin.saveResource(fileName, replace); 
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se pudo guardar el recurso por defecto: " + fileName + " (" + e.getMessage() + ")");
            }
        }
    }

    @Deprecated
    public FileConfiguration getConfig(String configName) {
        return legacyConfigs.computeIfAbsent(configName, name -> {
            var configFile = new File(plugin.getDataFolder(), name);
            return YamlConfiguration.loadConfiguration(configFile);
        });
    }

    @Deprecated
    public String getMessage(String path) {
        // 🌟 FIX: Reemplazo del obsoleto §c por Hex/MiniMessage compatible
        return getConfig("messages.yml").getString(path, "&#FF5555[!] Mensaje no encontrado: " + path);
    }
}
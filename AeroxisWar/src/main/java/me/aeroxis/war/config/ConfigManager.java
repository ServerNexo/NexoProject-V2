package me.aeroxis.war.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.war.AeroxisWar;
import me.aeroxis.war.config.nodes.WarMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 🏛️ Nexo Network - AeroxisWar Config Manager (Arquitectura Enterprise)
 * Gestor de configuraciones Type-Safe para el módulo de Guerra.
 */
@Singleton
public class ConfigManager {

    private final AeroxisWar plugin;
    private WarMessagesConfig messages;

    @Inject
    public ConfigManager(AeroxisWar plugin) {
        this.plugin = plugin;
        
        // Inicialización de archivos físicos
        saveDefaultResource("config.yml");
        saveDefaultResource("messages.yml");
        
        // Carga inicial del motor Configurate
        loadConfigurate();
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
            this.messages = node.get(WarMessagesConfig.class);
            
            // 🛡️ Fallback preventivo: Si el archivo está vacío o roto, instanciamos uno por defecto.
            if (this.messages == null) {
                plugin.getLogger().warning("⚠️ El archivo messages.yml de AeroxisWar parece estar vacío. Usando valores por defecto.");
                this.messages = new WarMessagesConfig(); 
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico al cargar messages.yml en AeroxisWar: " + e.getMessage());
        }
    }

    private void saveDefaultResource(String fileName) {
        var file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("No se pudo encontrar el recurso interno: " + fileName);
            }
        }
    }

    /**
     * 🔄 Recarga en caliente para el comando /war reload
     */
    public void reloadMessages() {
        loadConfigurate();
    }

    public WarMessagesConfig getMessages() {
        return messages;
    }
}
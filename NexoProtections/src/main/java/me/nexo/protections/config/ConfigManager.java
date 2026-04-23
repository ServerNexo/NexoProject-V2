package me.nexo.protections.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.protections.NexoProtections;
import me.nexo.protections.config.nodes.ProtectionsMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 🛡️ NexoProtections - Config Manager Purificado (Arquitectura Enterprise)
 * Rendimiento: Carga O(1) en RAM, Type-Safe, Fallback preventivo contra nulls.
 */
@Singleton
public class ConfigManager {

    private final NexoProtections plugin;
    private ProtectionsMessagesConfig messages;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoProtections plugin) {
        this.plugin = plugin;
        saveDefaultResource("messages.yml");
        loadConfigurate();
    }

    private void saveDefaultResource(String fileName) {
        var file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se pudo guardar " + fileName + " por defecto (" + e.getMessage() + ")");
            }
        }
    }

    private void loadConfigurate() {
        var file = new File(plugin.getDataFolder(), "messages.yml");
        var loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK) // Previene YAMLs ilegibles en 1 sola línea
                .build();
        try {
            var root = loader.load();
            this.messages = root.get(ProtectionsMessagesConfig.class);
            
            // 🛡️ Fallback preventivo: Salvaguarda contra YAMLs corruptos o vacíos
            if (this.messages == null) {
                plugin.getLogger().warning("⚠️ El archivo messages.yml de NexoProtections parece estar vacío. Usando valores por defecto.");
                this.messages = new ProtectionsMessagesConfig(); 
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico al cargar messages.yml en NexoProtections: " + e.getMessage());
            this.messages = new ProtectionsMessagesConfig(); // Fallback seguro anti-crasheos
        }
    }

    public void reloadMessages() {
        loadConfigurate();
    }

    // 💡 PILAR 2: Acceso Type-Safe a todos los textos (Cero I/O)
    public ProtectionsMessagesConfig getMessages() {
        return messages;
    }
}
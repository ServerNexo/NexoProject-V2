package me.nexo.mechanics.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.config.nodes.MechanicsMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * ⚙️ NexoMechanics - Config Manager Purificado (Arquitectura Enterprise)
 * Rendimiento: Type-Safe Configurate, Fallback Seguro y Manejo I/O Blindado.
 */
@Singleton
public class ConfigManager {

    private final NexoMechanics plugin;
    private MechanicsMessagesConfig messages;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoMechanics plugin) {
        this.plugin = plugin;
        saveDefaultResource("messages.yml");
        loadConfigurate();
    }

    private void saveDefaultResource(String fileName) {
        var file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false); // 🌟 Seguro: No borra datos si ya existe.
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se pudo guardar " + fileName + " por defecto (" + e.getMessage() + ")");
            }
        }
    }

    private void loadConfigurate() {
        var file = new File(plugin.getDataFolder(), "messages.yml");
        var loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            this.messages = loader.load().get(MechanicsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en NexoMechanics: " + e.getMessage());
            // 🌟 FIX: Fallback seguro. Evita NullPointerExceptions en todos los minijuegos si el YAML se corrompe.
            this.messages = new MechanicsMessagesConfig();
        }
    }

    public void reloadMessages() {
        // Mantenemos la recarga síncrona intencionalmente para evitar condiciones de carrera (Race Conditions)
        loadConfigurate();
    }

    // 💡 PILAR 2: Acceso Type-Safe
    public MechanicsMessagesConfig getMessages() {
        return messages;
    }
}
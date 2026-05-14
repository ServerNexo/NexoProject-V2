package me.aeroxis.minions.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.minions.AeroxisMinions;
import me.aeroxis.minions.config.nodes.MinionsMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 🤖 AeroxisMinions - Config Manager Purificado (Arquitectura Enterprise)
 * Rendimiento: Type-Safe Configurate, Fallback Seguro y Manejo I/O Blindado.
 */
@Singleton
public class ConfigManager {

    private final AeroxisMinions plugin;
    private MinionsMessagesConfig messages;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ConfigManager(AeroxisMinions plugin) {
        this.plugin = plugin;
        saveDefaultResource("messages.yml");
        saveDefaultResource("upgrades.yml");
        saveDefaultResource("tiers.yml");
        loadConfigurate();
    }

    private void saveDefaultResource(String fileName) {
        var file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.saveResource(fileName, false); // 🌟 Seguro: No borra datos.
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
            this.messages = loader.load().get(MinionsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en AeroxisMinions: " + e.getMessage());
            // 🌟 FIX: Fallback seguro. Evita NullPointerExceptions en todo el plugin si el YAML se corrompe.
            this.messages = new MinionsMessagesConfig();
        }
    }

    public void reloadMessages() {
        // La recarga se mantiene síncrona intencionalmente para evitar Race Conditions con los Comandos
        loadConfigurate();
    }

    // 💡 PILAR 2: Acceso Type-Safe
    public MinionsMessagesConfig getMessages() {
        return messages;
    }
}
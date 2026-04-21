package me.nexo.core.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoCore;
import me.nexo.core.config.nodes.MessagesConfig; // 🌟 Tu nueva clase mágica
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ Nexo Network - Configuration Manager (Arquitectura Enterprise)
 * Motor híbrido: Sponge Configurate (Nuevo) + Bukkit YAML (Legacy Bridge).
 */
@Singleton
public class ConfigManager {

    private final NexoCore plugin;
    private final Map<String, FileConfiguration> legacyConfigs = new ConcurrentHashMap<>();

    // 💡 PILAR 2: Nuestra Configuración Type-Safe
    private MessagesConfig messages;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoCore plugin) {
        this.plugin = plugin;

        // 1. Extraer archivos físicos si no existen
        // 🚨 FIX CRÍTICO: replace en 'false' para NO borrar la configuración del usuario al reiniciar
        saveDefaultResource("config.yml", false);
        saveDefaultResource("messages.yml", false);

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
        var file = new File(plugin.getDataFolder(), "messages.yml");
        var loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            // ¡MAGIA! Lee el YAML y lo transforma en nuestro objeto Java
            var node = loader.load();
            this.messages = node.get(MessagesConfig.class);
            
            if (this.messages == null) {
                plugin.getLogger().warning("⚠️ El archivo messages.yml está vacío o mal formateado. Se usarán valores por defecto.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico al cargar messages.yml: " + e.getMessage());
        }
    }

    public MessagesConfig getMessages() {
        return messages;
    }

    // ==========================================
    // 🌉 PUENTE LEGACY Y UTILIDADES
    // ==========================================

    private void saveDefaultResource(String fileName, boolean replace) {
        var file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists() || replace) {
            try { 
                plugin.saveResource(fileName, replace); 
            } catch (IllegalArgumentException ignored) {
                // Se ignora si el recurso no existe en el .jar
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
        return getMessage("messages.yml", path);
    }

    @Deprecated
    public String getMessage(String configName, String path) {
        // Mantenemos el String plano para retrocompatibilidad con módulos viejos
        return getConfig(configName).getString(path, "&cMensaje no encontrado: " + path);
    }

    public void reloadConfigs() {
        // 🚨 FIX CRÍTICO: replace en 'false' para no sobreescribir datos en el comando /reload
        saveDefaultResource("messages.yml", false); 
        loadConfigurate(); // Recarga el nuevo motor
        legacyConfigs.clear(); // Limpia la caché vieja para que se vuelva a cargar
        getConfig("config.yml");
        getConfig("messages.yml");
    }
}
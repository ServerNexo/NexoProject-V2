package me.nexo.dungeons.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.config.nodes.DungeonsMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 🏰 NexoDungeons - Gestor de Configuración Tipado (Arquitectura Enterprise Java 21)
 * Rendimiento: Carga síncrona segura, Fallbacks en Memoria y Type-Safe.
 */
@Singleton
public class ConfigManager {

    private final NexoDungeons plugin;
    private DungeonsMessagesConfig messages;
    private YamlConfigurationLoader messagesLoader;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoDungeons plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        var file = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!file.exists()) {
            try {
                // 🌟 FIX: Blindaje contra ausencia de archivos en el JAR compilado
                plugin.saveResource("messages.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se encontró 'messages.yml' en el JAR. Usando valores en memoria.");
            }
        }

        var path = file.toPath();
        
        // 🌟 FIX: Forzamos el estilo BLOCK para que el YAML sea legible por humanos
        this.messagesLoader = YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        try {
            var root = messagesLoader.load();
            this.messages = root.get(DungeonsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico cargando messages.yml de Dungeons: " + e.getMessage());
            this.messages = new DungeonsMessagesConfig(); // Fallback seguro
        }
    }

    // 🌟 Acceso Type-Safe a los textos (Adiós al viejo getMessage)
    public DungeonsMessagesConfig getMessages() {
        return messages;
    }
}
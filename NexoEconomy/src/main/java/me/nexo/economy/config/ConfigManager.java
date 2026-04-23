package me.nexo.economy.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.config.nodes.EconomyMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 💰 NexoEconomy - Gestor de Configuración Tipado (Arquitectura Enterprise Java 21)
 * Rendimiento: Carga síncrona segura, Fallbacks en Memoria y Type-Safe.
 */
@Singleton
public class ConfigManager {

    private final NexoEconomy plugin;
    private EconomyMessagesConfig messages;
    private YamlConfigurationLoader messagesLoader;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public ConfigManager(NexoEconomy plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        var file = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!file.exists()) {
            try {
                // 🌟 FIX: Blindaje contra ausencia de recursos en el JAR compilado
                plugin.saveResource("messages.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se encontró 'messages.yml' en el JAR. Generando valores predeterminados en memoria.");
            }
        }

        var path = file.toPath();
        
        // 🌟 FIX: Forzamos el estilo BLOCK para que el YAML sea legible por humanos
        this.messagesLoader = YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        try {
            // Carga el archivo y lo mapea automáticamente a nuestra clase Type-Safe
            var root = messagesLoader.load();
            this.messages = root.get(EconomyMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico cargando messages.yml de Economy: " + e.getMessage());
            this.messages = new EconomyMessagesConfig(); // Fallback seguro para que no crashee
        }
    }

    // 🌟 Acceso Type-Safe a los textos (Reemplaza al viejo getMessage)
    public EconomyMessagesConfig getMessages() {
        return messages;
    }
}
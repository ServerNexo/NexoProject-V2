package me.nexo.clans.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.clans.NexoClans;
import me.nexo.clans.config.nodes.ClansMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 👥 NexoClans - Gestor de Configuración Tipado (Arquitectura Enterprise)
 * Rendimiento: Carga O(1) en Memoria RAM, Prevención de NullPointers y Formato Limpio.
 */
@Singleton
public class ConfigManager {

    private final NexoClans plugin;
    private ClansMessagesConfig messages;
    private YamlConfigurationLoader messagesLoader;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoClans plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        var file = new File(plugin.getDataFolder(), "messages.yml");

        // 🌟 FIX SEGURIDAD: Usamos 'false' para NO borrar los cambios que hagan los admins.
        if (!file.exists()) {
            try {
                plugin.saveResource("messages.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se pudo guardar messages.yml por defecto (" + e.getMessage() + ")");
            }
        }

        // 🌟 FIX FORMATO: Forzamos el estilo de bloque para evitar YAML de una sola línea
        messagesLoader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        try {
            // Leemos todo el archivo UNA SOLA VEZ y lo guardamos en RAM usando Nodos Tipados
            var root = messagesLoader.load();
            this.messages = root.get(ClansMessagesConfig.class);
            
            // 🛡️ Fallback preventivo: Salvaguarda contra YAMLs corruptos o vacíos
            if (this.messages == null) {
                plugin.getLogger().warning("⚠️ El archivo messages.yml de NexoClans parece estar vacío. Usando valores por defecto.");
                this.messages = new ClansMessagesConfig(); 
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico cargando messages.yml de Clanes: " + e.getMessage());
            this.messages = new ClansMessagesConfig(); // Fallback seguro para evitar crasheos en la red
        }
    }

    // 🌟 Acceso Type-Safe a los textos (Adiós al viejo getConfig().getString() )
    public ClansMessagesConfig getMessages() {
        return messages;
    }
}
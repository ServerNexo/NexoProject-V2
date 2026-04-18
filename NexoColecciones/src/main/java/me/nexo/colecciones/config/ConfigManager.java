package me.nexo.colecciones.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.config.nodes.ColeccionesMessagesConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;

/**
 * 📚 NexoColecciones - Gestor de Configuración Tipado (Arquitectura Enterprise)
 */
@Singleton
public class ConfigManager {

    private final NexoColecciones plugin;
    private ColeccionesMessagesConfig messages;
    private YamlConfigurationLoader messagesLoader;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoColecciones plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");

        // 🌟 FIX SEGURIDAD: Usamos 'false' para NO borrar los cambios que hagan los admins en el archivo.
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        Path path = file.toPath();
        messagesLoader = YamlConfigurationLoader.builder().path(path).build();

        try {
            // Leemos todo el archivo UNA SOLA VEZ y lo guardamos en RAM usando Nodos Tipados
            CommentedConfigurationNode root = messagesLoader.load();
            messages = root.get(ColeccionesMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico cargando messages.yml de Colecciones: " + e.getMessage());
            messages = new ColeccionesMessagesConfig(); // Fallback seguro
        }
    }

    // 🌟 Acceso Type-Safe a los textos (Adiós al viejo getMessage)
    public ColeccionesMessagesConfig getMessages() {
        return messages;
    }
}
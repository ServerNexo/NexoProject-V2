package me.nexo.economy.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.economy.NexoEconomy;
import me.nexo.economy.config.nodes.EconomyMessagesConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;

/**
 * 💰 NexoEconomy - Gestor de Configuración Tipado (Arquitectura Enterprise)
 */
@Singleton
public class ConfigManager {

    private final NexoEconomy plugin;
    private EconomyMessagesConfig messages;
    private YamlConfigurationLoader messagesLoader;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoEconomy plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            // Genera el archivo por defecto si no existe
            plugin.saveResource("messages.yml", false);
        }

        Path path = file.toPath();
        messagesLoader = YamlConfigurationLoader.builder().path(path).build();

        try {
            // Carga el archivo y lo mapea automáticamente a nuestra clase Type-Safe
            CommentedConfigurationNode root = messagesLoader.load();
            messages = root.get(EconomyMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico cargando messages.yml de Economy: " + e.getMessage());
            messages = new EconomyMessagesConfig(); // Fallback seguro para que no crashee
        }
    }

    // 🌟 Acceso Type-Safe a los textos (Reemplaza al viejo getMessage)
    public EconomyMessagesConfig getMessages() {
        return messages;
    }
}
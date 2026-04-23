package me.nexo.factories.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.factories.NexoFactories;
import me.nexo.factories.config.nodes.FactoriesMessagesConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏭 NexoFactories - Gestor de Configuración Tipado (Arquitectura Enterprise Java 21)
 * Rendimiento: Recargas asíncronas I/O con Virtual Threads, variables volátiles y Type-Safe.
 */
@Singleton
public class ConfigManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoFactories plugin;
    
    // 🛡️ CONCURRENCIA: 'volatile' garantiza que todos los hilos vean la última versión tras un reload asíncrono
    private volatile FactoriesMessagesConfig messages;
    private YamlConfigurationLoader messagesLoader;

    // 🚀 MOTOR I/O: Hilos Virtuales para recargas en caliente sin lag
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public ConfigManager(NexoFactories plugin) {
        this.plugin = plugin;
        
        // Carga inicial síncrona obligatoria para tener los datos listos durante el arranque (onEnable)
        loadMessagesSync();
    }

    private void loadMessagesSync() {
        var file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        var path = file.toPath();
        messagesLoader = YamlConfigurationLoader.builder().path(path).build();

        try {
            CommentedConfigurationNode root = messagesLoader.load();
            messages = root.get(FactoriesMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error cargando messages.yml de Factories: " + e.getMessage());
            messages = new FactoriesMessagesConfig(); // Fallback seguro para evitar NullPointerExceptions
        }
    }

    /**
     * 🛡️ Recarga Asíncrona: Permite ejecutar comandos de recarga sin congelar el servidor.
     */
    public void reloadMessages() {
        virtualExecutor.execute(this::loadMessagesSync);
    }

    // 💡 Acceso Type-Safe Ultra Rápido O(1)
    public FactoriesMessagesConfig getMessages() {
        return messages;
    }
}
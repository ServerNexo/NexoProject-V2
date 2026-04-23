package me.nexo.items.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import me.nexo.items.config.nodes.ItemsMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🎒 NexoItems - Config Manager Purificado (Arquitectura Enterprise Java 21)
 * Rendimiento: Virtual Threads para recargas I/O, variables volátiles y acceso Type-Safe O(1).
 */
@Singleton
public class ConfigManager {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    
    // 🛡️ CONCURRENCIA: 'volatile' garantiza que todos los hilos vean la última versión tras un reload
    private volatile ItemsMessagesConfig messages;

    // 🚀 MOTOR I/O: Hilos Virtuales para recargas en caliente sin lag
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 💉 PILAR 1: Inyección de Dependencias Estricta
    @Inject
    public ConfigManager(NexoItems plugin) {
        this.plugin = plugin;
        
        // Carga inicial síncrona obligatoria para tener los datos listos antes de registrar Listeners
        cargarArchivosSync();
    }

    private void cargarArchivosSync() {
        // Generamos todos los archivos del módulo
        saveDefaultResource("messages.yml");
        saveDefaultResource("armas.yml");
        saveDefaultResource("armaduras.yml");
        saveDefaultResource("artefactos.yml");
        saveDefaultResource("herramientas.yml");
        saveDefaultResource("encantamientos.yml");
        saveDefaultResource("reforjas.yml");

        loadConfigurate();
    }

    private void saveDefaultResource(String fileName) {
        var file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private void loadConfigurate() {
        var file = new File(plugin.getDataFolder(), "messages.yml");
        var loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        try {
            this.messages = loader.load().get(ItemsMessagesConfig.class);
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error al cargar messages.yml en NexoItems: " + e.getMessage());
        }
    }

    /**
     * 🛡️ Recarga Asíncrona: Permite ejecutar /nexoitems reload sin congelar el servidor.
     */
    public void reloadMessages() {
        virtualExecutor.execute(this::loadConfigurate);
    }

    // 💡 PILAR 2: Acceso Type-Safe Ultra Rápido O(1)
    public ItemsMessagesConfig getMessages() {
        return messages;
    }
}
package me.nexo.colecciones.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.config.nodes.ColeccionesMessagesConfig;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;

/**
 * 📚 NexoColecciones - Gestor de Configuración Tipado (Arquitectura Enterprise)
 * Rendimiento: Carga O(1) en RAM, Type-Safe, Fallback preventivo contra nulls.
 */
@Singleton
public class ConfigManager {

    private final NexoColecciones plugin;
    private ColeccionesMessagesConfig messages;

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ConfigManager(NexoColecciones plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        var file = new File(plugin.getDataFolder(), "messages.yml");

        // 🌟 FIX SEGURIDAD: Usamos 'false' para NO borrar los cambios que hagan los admins,
        // y protegemos contra IllegalArgumentException si el archivo no existe en el JAR compilado.
        if (!file.exists()) {
            try {
                plugin.saveResource("messages.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se pudo guardar messages.yml por defecto (" + e.getMessage() + ")");
            }
        }

        var loader = YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK) // 🌟 FIX: Previene YAMLs aplanados/ilegibles
                .build();

        try {
            // Leemos todo el archivo UNA SOLA VEZ y lo guardamos en RAM usando Nodos Tipados
            var root = loader.load();
            this.messages = root.get(ColeccionesMessagesConfig.class);

            // 🛡️ Fallback preventivo: Salvaguarda contra YAMLs corruptos o vacíos
            if (this.messages == null) {
                plugin.getLogger().warning("⚠️ El archivo messages.yml de NexoColecciones parece estar vacío. Usando valores por defecto.");
                this.messages = new ColeccionesMessagesConfig();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error crítico cargando messages.yml de Colecciones: " + e.getMessage());
            this.messages = new ColeccionesMessagesConfig(); // Fallback seguro
        }
    }

    // 🌟 Añadido método de recarga para estandarizar con otros módulos
    public void reloadMessages() {
        loadMessages();
    }

    // 🌟 Acceso Type-Safe a los textos (Adiós al viejo getMessage)
    public ColeccionesMessagesConfig getMessages() {
        return messages;
    }
}
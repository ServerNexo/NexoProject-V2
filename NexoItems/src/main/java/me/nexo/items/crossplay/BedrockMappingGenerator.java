package me.nexo.items.crossplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.items.NexoItems;
import me.nexo.items.managers.FileManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🎒 NexoItems - Generador de Mapeos GeyserMC (Arquitectura Enterprise Java 21)
 * Rendimiento: Virtual Threads unificados, Reutilización GSON e Inyección Estricta.
 */
@Singleton
public class BedrockMappingGenerator {

    // 🌟 DEPENDENCIAS PROPAGADAS
    private final NexoItems plugin;
    private final FileManager fileManager;

    // 🚀 MOTOR I/O: Hilos Virtuales para escritura de archivos JSON
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    // 🌟 GSON CACHEADO: Creado una sola vez, 100% Thread-Safe para serializar
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // 💉 PILAR 1: Inyección Estricta
    @Inject
    public BedrockMappingGenerator(NexoItems plugin, FileManager fileManager) {
        this.plugin = plugin;
        this.fileManager = fileManager;
    }

    /**
     * 🛡️ Inicia el escaneo de YAMLs y construye el archivo de mapeo para GeyserMC.
     * Se ejecuta de forma asíncrona para no detener el arranque del servidor.
     */
    public void generarMappings() {
        virtualExecutor.execute(() -> {
            plugin.getLogger().info("[Cross-Play] Iniciando generación de Mappings 2D para Bedrock...");

            var root = new JsonObject();
            root.addProperty("format_version", "1");
            var items = new JsonObject();

            // 1. Escanear Armas RPG
            var configArmas = fileManager.getArmas();
            if (configArmas.contains("armas_rpg")) {
                for (String key : configArmas.getConfigurationSection("armas_rpg").getKeys(false)) {
                    mapearItem(items, configArmas, "armas_rpg." + key);
                }
            }

            // 2. Escanear Herramientas Profesión
            var configTools = fileManager.getHerramientas();
            if (configTools.contains("herramientas")) {
                for (String key : configTools.getConfigurationSection("herramientas").getKeys(false)) {
                    mapearItem(items, configTools, "herramientas." + key);
                }
            }

            root.add("items", items);

            // 3. Exportar el JSON
            guardarArchivoJson(root);
        });
    }

    private void mapearItem(JsonObject itemsRoot, FileConfiguration config, String path) {
        String materialJava = config.getString(path + ".material", "IRON_SWORD").toLowerCase();
        int cmd = config.getInt(path + ".custom_model_data", 0);
        String texturaBedrock = config.getString(path + ".textura_2d", "default_texture");

        if (cmd == 0) return; // Si no tiene modelo custom, lo ignoramos

        String minecraftMat = "minecraft:" + materialJava;

        // Obtenemos el array de ese material (por si hay múltiples espadas de hierro con distinto CMD)
        JsonArray materialArray;
        if (itemsRoot.has(minecraftMat)) {
            materialArray = itemsRoot.getAsJsonArray(minecraftMat);
        } else {
            materialArray = new JsonArray();
            itemsRoot.add(minecraftMat, materialArray);
        }

        // Construimos la regla de mapeo
        var mappingRule = new JsonObject();

        // El nombre identificador interno
        mappingRule.addProperty("name", "nexo_network:" + texturaBedrock);

        // Coincidencia estricta: Solo si el CustomModelData de Java coincide
        mappingRule.addProperty("custom_model_data", cmd);

        // 🌟 REGLA DE OPTIMIZACIÓN: Le decimos a Geyser que asigne la textura 2D HD
        mappingRule.addProperty("texture", texturaBedrock);

        // 🌟 UNBREAKABLE FLAG: Evita el "Jitter" (parpadeo) de la barra de durabilidad en Bedrock
        // (En el ItemManager de Java ya le ponemos setUnbreakable(true), esto sincroniza visualmente a Geyser)
        mappingRule.addProperty("allow_offhand", true);

        materialArray.add(mappingRule);
    }

    private void guardarArchivoJson(JsonObject root) {
        // Guardamos el archivo en la carpeta de NexoItems.
        // El administrador solo tendrá que moverlo a la carpeta Geyser-Spigot/custom_mappings/
        var file = new File(plugin.getDataFolder(), "bedrock_custom_mappings.json");
        try {
            String jsonOutput = gson.toJson(root);

            try (var writer = new FileWriter(file)) {
                writer.write(jsonOutput);
            }
            plugin.getLogger().info("[✓] ¡bedrock_custom_mappings.json generado con éxito!");

        } catch (IOException e) {
            plugin.getLogger().severe("[-] Error al generar el mapping de Bedrock: " + e.getMessage());
        }
    }
}
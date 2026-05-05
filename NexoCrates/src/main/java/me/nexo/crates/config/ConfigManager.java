package me.nexo.crates.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.crates.NexoCrates;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Singleton
public class ConfigManager {

    private final NexoCrates plugin;
    private CommentedConfigurationNode cratesNode;
    private YamlConfigurationLoader cratesLoader;

    @Inject
    public ConfigManager(NexoCrates plugin) {
        this.plugin = plugin;
        // 🌟 FIX: Ya no cargamos los archivos aquí adentro para evitar el [this-escape]
    }

    public void loadConfigs() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File cratesFile = new File(dataFolder, "crates.yml");
        if (!cratesFile.exists()) {
            saveResource("crates.yml", cratesFile);
        }

        this.cratesLoader = YamlConfigurationLoader.builder()
                .path(cratesFile.toPath())
                .build();

        try {
            this.cratesNode = cratesLoader.load();
            plugin.getLogger().info("✅ Archivo crates.yml cargado exitosamente.");
        } catch (ConfigurateException e) {
            plugin.getLogger().severe("❌ Error al cargar crates.yml: " + e.getMessage());
        }
    }

    private void saveResource(String resourcePath, File outFile) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                Files.copy(in, outFile.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CommentedConfigurationNode getCratesNode() {
        return cratesNode;
    }

    public void reloadConfigs() {
        loadConfigs();
    }
}
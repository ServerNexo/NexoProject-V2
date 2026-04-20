package me.nexo.minions.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.minions.NexoMinions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

// 🌟 FIX: Declaramos que es Singleton para que solo cargue el YML una vez y no genere lag
@Singleton
public class TiersConfig {
    private final NexoMinions plugin;
    private FileConfiguration config;

    @Inject // 🌟 FIX: Inyección habilitada
    public TiersConfig(NexoMinions plugin) {
        this.plugin = plugin;
        cargarConfig();
    }

    // 🌟 CAMBIO CLAVE: Ahora es PUBLIC para que el comando Reload pueda usarlo
    public void cargarConfig() {
        File configFile = new File(plugin.getDataFolder(), "tiers.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("tiers.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public ConfigurationSection getCostoEvolucion(MinionType type, int tier) {
        if (config == null) return null;
        // 🌟 Ahora busca el costo ESPECÍFICO de ese tipo de minion
        return config.getConfigurationSection("tiers." + tier + ".costo_evolucion." + type.name());
    }
}
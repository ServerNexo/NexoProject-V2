package me.nexo.minions.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.minions.NexoMinions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * 🤖 NexoMinions - Configuración de Tiers y Evoluciones (Arquitectura Enterprise)
 * Rendimiento: Singleton Estricto, Manejo I/O Seguro y Acceso Directo O(1).
 */
@Singleton
public class TiersConfig {
    
    private final NexoMinions plugin;
    private FileConfiguration config;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public TiersConfig(NexoMinions plugin) {
        this.plugin = plugin;
        cargarConfig();
    }

    // 🌟 SELLO ROTO: Ahora es PUBLIC para que el comando Reload pueda usarlo
    public void cargarConfig() {
        var configFile = new File(plugin.getDataFolder(), "tiers.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                plugin.saveResource("tiers.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se pudo guardar tiers.yml por defecto (" + e.getMessage() + ")");
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public ConfigurationSection getCostoEvolucion(MinionType type, int tier) {
        if (config == null) return null;
        // 🌟 Ahora busca el costo ESPECÍFICO de ese tipo de minion
        return config.getConfigurationSection("tiers." + tier + ".costo_evolucion." + type.name());
    }
}
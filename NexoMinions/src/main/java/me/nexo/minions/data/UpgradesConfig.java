package me.nexo.minions.data;

import me.nexo.minions.NexoMinions;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;

public class UpgradesConfig {
    private final NexoMinions plugin;
    private FileConfiguration config;

    public UpgradesConfig(NexoMinions plugin) {
        this.plugin = plugin;
        cargarConfig();
    }

    // 🌟 SELLO ROTO: Ahora es PUBLIC para permitir el ritual de recarga
    public void cargarConfig() {
        File configFile = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("upgrades.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // 🌟 LA NUEVA MAGIA: Extraemos la ID de Nexo leyendo la etiqueta oculta (Bypass de la API)
    private String getNexoId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey nexoKey = new NamespacedKey("nexo", "id");
        return item.getItemMeta().getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
    }

    // Identifica qué mejora es el ItemStack
    public ConfigurationSection getUpgradeData(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        // Obtenemos la ID de manera 100% segura
        String nexoId = getNexoId(item);

        ConfigurationSection upgradesSec = config.getConfigurationSection("upgrades");
        if (upgradesSec == null) return null;

        for (String key : upgradesSec.getKeys(false)) {
            ConfigurationSection sec = upgradesSec.getConfigurationSection(key);
            if (sec == null) continue;

            // 1. Busca por ID de Nexo (Para ítems custom)
            if (nexoId != null && nexoId.equals(sec.getString("nexo_id", ""))) {
                return sec;
            }
            // 2. Fallback: Busca por Material de Vanilla
            if (item.getType().name().equalsIgnoreCase(sec.getString("material", ""))) {
                return sec;
            }
        }
        return null;
    }
}
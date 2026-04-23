package me.nexo.minions.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.minions.NexoMinions;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;

/**
 * 🤖 NexoMinions - Configuración de Mejoras (Arquitectura Enterprise)
 * Rendimiento: Llave cacheada O(1), Singleton Estricto, Manejo I/O Seguro.
 */
@Singleton
public class UpgradesConfig {
    private final NexoMinions plugin;
    private FileConfiguration config;

    // 🌟 OPTIMIZACIÓN O(1): Cacheamos la llave de Nexo para evitar memory leaks
    private final NamespacedKey nexoKey;

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public UpgradesConfig(NexoMinions plugin) {
        this.plugin = plugin;
        this.nexoKey = new NamespacedKey("nexo", "id"); // Instanciada una sola vez
        cargarConfig();
    }

    // 🌟 SELLO ROTO: Ahora es PUBLIC para permitir el ritual de recarga
    public void cargarConfig() {
        var configFile = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                plugin.saveResource("upgrades.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se pudo guardar upgrades.yml por defecto (" + e.getMessage() + ")");
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // 🌟 LA NUEVA MAGIA: Extraemos la ID de Nexo leyendo la etiqueta oculta (Bypass de la API)
    private String getNexoId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(nexoKey, PersistentDataType.STRING);
    }

    // Identifica qué mejora es el ItemStack
    public ConfigurationSection getUpgradeData(ItemStack item) {
        // 🌟 PAPER 1.21 FIX: isEmpty() es la forma nativa y segura de verificar aire/nulos
        if (item == null || item.isEmpty()) return null; 

        // Obtenemos la ID de manera 100% segura
        String nexoId = getNexoId(item);

        var upgradesSec = config.getConfigurationSection("upgrades");
        if (upgradesSec == null) return null;

        for (String key : upgradesSec.getKeys(false)) {
            var sec = upgradesSec.getConfigurationSection(key);
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
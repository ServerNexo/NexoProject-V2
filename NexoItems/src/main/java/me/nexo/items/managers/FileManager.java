package me.nexo.items.managers;

import me.nexo.items.NexoItems;
import me.nexo.items.dtos.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {

    // 🟢 ARQUITECTURA: Ahora usamos la clase principal de NexoItems
    private final NexoItems plugin;

    private FileConfiguration artefactosConfig, armadurasConfig, armasConfig, herramientasConfig, reforjasConfig, encantamientosConfig;
    private File artefactosFile, armadurasFile, armasFile, herramientasFile, reforjasFile, encantamientosFile;

    // 🧠 CACHÉ EN RAM PARA ARMADURAS, ARMAS, HERRAMIENTAS, REFORJAS Y ENCANTAMIENTOS
    private final ConcurrentHashMap<String, ArmorDTO> armorCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WeaponDTO> weaponCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ToolDTO> toolCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReforgeDTO> reforgeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EnchantDTO> enchantCache = new ConcurrentHashMap<>();

    public FileManager(NexoItems plugin) {
        this.plugin = plugin;
        cargarArchivos();
    }

    public void cargarArchivos() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        artefactosFile = new File(plugin.getDataFolder(), "artefactos.yml");
        if (!artefactosFile.exists()) plugin.saveResource("artefactos.yml", false);
        artefactosConfig = YamlConfiguration.loadConfiguration(artefactosFile);

        armadurasFile = new File(plugin.getDataFolder(), "armaduras.yml");
        if (!armadurasFile.exists()) plugin.saveResource("armaduras.yml", false);
        armadurasConfig = YamlConfiguration.loadConfiguration(armadurasFile);

        armasFile = new File(plugin.getDataFolder(), "armas.yml");
        if (!armasFile.exists()) plugin.saveResource("armas.yml", false);
        armasConfig = YamlConfiguration.loadConfiguration(armasFile);

        herramientasFile = new File(plugin.getDataFolder(), "herramientas.yml");
        if (!herramientasFile.exists()) plugin.saveResource("herramientas.yml", false);
        herramientasConfig = YamlConfiguration.loadConfiguration(herramientasFile);

        reforjasFile = new File(plugin.getDataFolder(), "reforjas.yml");
        if (!reforjasFile.exists()) plugin.saveResource("reforjas.yml", false);
        reforjasConfig = YamlConfiguration.loadConfiguration(reforjasFile);

        encantamientosFile = new File(plugin.getDataFolder(), "encantamientos.yml");
        if (!encantamientosFile.exists()) plugin.saveResource("encantamientos.yml", false);
        encantamientosConfig = YamlConfiguration.loadConfiguration(encantamientosFile);

        // Cargamos los datos a la RAM
        cargarCacheArmaduras();
        cargarCacheArmas();
        cargarCacheHerramientas();
        cargarCacheReforjas();
        cargarCacheEncantamientos();
    }

    private void cargarCacheArmaduras() {
        armorCache.clear();
        if (armadurasConfig.contains("armaduras_profesion")) {
            for (String key : armadurasConfig.getConfigurationSection("armaduras_profesion").getKeys(false)) {
                String path = "armaduras_profesion." + key;
                ArmorDTO dto = new ArmorDTO(
                        key,
                        armadurasConfig.getString(path + ".nombre", "Armadura"),
                        armadurasConfig.getString(path + ".clase", "Cualquiera"),
                        armadurasConfig.getString(path + ".requisito_skill", "Ninguna"),
                        armadurasConfig.getInt(path + ".nivel_requerido", 1),
                        armadurasConfig.getDouble(path + ".vida_extra", 0.0),
                        armadurasConfig.getDouble(path + ".velocidad_movimiento", 0.0),
                        armadurasConfig.getDouble(path + ".suerte_minera", 0.0),
                        armadurasConfig.getDouble(path + ".velocidad_mineria", 0.0),
                        armadurasConfig.getDouble(path + ".suerte_agricola", 0.0),
                        armadurasConfig.getDouble(path + ".suerte_tala", 0.0),
                        armadurasConfig.getDouble(path + ".criatura_marina", 0.0),
                        armadurasConfig.getDouble(path + ".velocidad_pesca", 0.0)
                );
                armorCache.put(key, dto);
            }
        }
    }

    private void cargarCacheArmas() {
        weaponCache.clear();
        if (armasConfig.contains("armas_rpg")) {
            for (String key : armasConfig.getConfigurationSection("armas_rpg").getKeys(false)) {
                String path = "armas_rpg." + key;
                WeaponDTO dto = new WeaponDTO(
                        key,
                        armasConfig.getString(path + ".nombre", "Arma"),
                        armasConfig.getInt(path + ".tier", 1),
                        armasConfig.getString(path + ".clase", "Cualquiera"),
                        armasConfig.getString(path + ".elemento", "FÍSICO"),
                        armasConfig.getInt(path + ".nivel_requerido", 1),
                        armasConfig.getDouble(path + ".danio_base", 5.0),
                        armasConfig.getDouble(path + ".velocidad_ataque", 1.6),
                        armasConfig.getString(path + ".habilidad_id", "ninguna"),
                        armasConfig.getBoolean(path + ".prestigio.habilitado", false),
                        armasConfig.getDouble(path + ".prestigio.multiplicador", 0.1)
                );
                weaponCache.put(key, dto);
            }
        }
    }

    private void cargarCacheHerramientas() {
        toolCache.clear();
        if (herramientasConfig.contains("herramientas")) {
            for (String key : herramientasConfig.getConfigurationSection("herramientas").getKeys(false)) {
                String path = "herramientas." + key;
                ToolDTO dto = new ToolDTO(
                        key,
                        herramientasConfig.getString(path + ".nombre", "Herramienta"),
                        herramientasConfig.getString(path + ".rareza", "&7Común"),
                        herramientasConfig.getString(path + ".profesion", "Minería"),
                        herramientasConfig.getInt(path + ".tier", 1),
                        herramientasConfig.getInt(path + ".nivel_requerido", 1),
                        herramientasConfig.getDouble(path + ".velocidad_base", 1.0),
                        herramientasConfig.getDouble(path + ".multiplicador_fortuna", 0.0),
                        herramientasConfig.getString(path + ".habilidad_id", "ninguna")
                );
                toolCache.put(key, dto);
            }
        }
    }

    public void cargarCacheReforjas() {
        reforgeCache.clear();
        if (reforjasConfig.contains("reforjas")) {
            for (String key : reforjasConfig.getConfigurationSection("reforjas").getKeys(false)) {
                String path = "reforjas." + key;
                ReforgeDTO dto = new ReforgeDTO(
                        key,
                        reforjasConfig.getString(path + ".nombre", "Reforjado"),
                        reforjasConfig.getString(path + ".prefijo_color", "&7"),
                        reforjasConfig.getStringList(path + ".aplica_a"),
                        reforjasConfig.getInt(path + ".costo_polvo", 1),
                        reforjasConfig.getDouble(path + ".stats.danio_extra", 0.0),
                        reforjasConfig.getDouble(path + ".stats.velocidad_ataque_extra", 0.0),
                        reforjasConfig.getDouble(path + ".stats.fortuna", 0.0)
                );
                reforgeCache.put(key, dto);
            }
        }
    }

    public void cargarCacheEncantamientos() {
        enchantCache.clear();
        if (encantamientosConfig.contains("encantamientos")) {
            for (String key : encantamientosConfig.getConfigurationSection("encantamientos").getKeys(false)) {
                String path = "encantamientos." + key;
                EnchantDTO dto = new EnchantDTO(
                        key,
                        encantamientosConfig.getString(path + ".nombre", "Encantamiento Desconocido"),
                        encantamientosConfig.getInt(path + ".nivel_maximo", 1),
                        encantamientosConfig.getStringList(path + ".aplica_a"),
                        encantamientosConfig.getString(path + ".descripcion", ""),
                        encantamientosConfig.getDoubleList(path + ".valores_por_nivel")
                );
                enchantCache.put(key, dto);
            }
        }
    }

    // GETTERS
    public ArmorDTO getArmorDTO(String id) { return armorCache.get(id); }
    public WeaponDTO getWeaponDTO(String id) { return weaponCache.get(id); }
    public ToolDTO getToolDTO(String id) { return toolCache.get(id); }
    public ReforgeDTO getReforgeDTO(String id) { return reforgeCache.get(id); }
    public EnchantDTO getEnchantDTO(String id) { return enchantCache.get(id); }

    public FileConfiguration getArtefactos() { return artefactosConfig; }
    public FileConfiguration getArmaduras() { return armadurasConfig; }
    public FileConfiguration getArmas() { return armasConfig; }
    public FileConfiguration getHerramientas() { return herramientasConfig; }
    public FileConfiguration getReforjas() { return reforjasConfig; }
    public FileConfiguration getEncantamientos() { return encantamientosConfig; }
}
package me.nexo.colecciones.colecciones;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.colecciones.data.Tier;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 📚 NexoColecciones - Lector de Datos de Colecciones (Arquitectura Enterprise)
 * Rendimiento: Carga en RAM O(1), I/O Asíncrono Gestionado y Manejo de Errores Seguro.
 */
@Singleton
public class ColeccionesConfig {

    private final NexoColecciones plugin;
    private FileConfiguration config;
    private File configFile;
    
    // 🌟 MOTOR ENTERPRISE: Executor formal para tareas de I/O de disco
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public ColeccionesConfig(NexoColecciones plugin) {
        this.plugin = plugin;
        crearConfig();
    }

    public void crearConfig() {
        configFile = new File(plugin.getDataFolder(), "colecciones.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                plugin.saveResource("colecciones.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("⚠️ No se pudo guardar colecciones.yml por defecto (" + e.getMessage() + ")");
            }
        }

        // Carga inicial (Síncrona por necesidad de arranque)
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // 🌟 FIX: Guardado Asíncrono Seguro (Evita corrupción de YAML en apagados)
    public void guardarConfigAsync() {
        ioExecutor.submit(() -> {
            try {
                config.save(configFile);
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error guardando colecciones.yml: " + e.getMessage());
            }
        });
    }

    public void recargarConfig() {
        // Se mantiene síncrono para evitar Race Conditions con el ComandoColecciones 
        // que depende de que esto termine antes de recargar la RAM.
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // ==========================================================
    // 🦇 MOTOR DE ENSAMBLAJE (CARGADO EN RAM O(1))
    // ==========================================================

    /**
     * Lee el colecciones.yml y construye todas las Categorías, Colecciones y Tiers en memoria.
     */
    public Map<String, CollectionCategory> cargarCategoriasEnRam() {
        Map<String, CollectionCategory> categoriasMap = new HashMap<>();

        // 1. Leer Categorías
        var catSec = config.getConfigurationSection("categorias");
        if (catSec != null) {
            for (String key : catSec.getKeys(false)) {
                String nombre = catSec.getString(key + ".nombre", "&#FFAA00" + key);

                // matchMaterial es más seguro que getMaterial en Paper 1.21+
                var icono = Material.matchMaterial(catSec.getString(key + ".icono", "STONE").toUpperCase());
                if (icono == null) icono = Material.STONE;
                int slot = catSec.getInt(key + ".slot", 0);

                categoriasMap.put(key, new CollectionCategory(key, nombre, icono, slot));
            }
        }

        // 2. Leer Colecciones y Asignarlas a su Categoría
        var colSec = config.getConfigurationSection("colecciones");
        if (colSec != null) {
            for (String colKey : colSec.getKeys(false)) {
                String catId = colSec.getString(colKey + ".categoria", "");
                var categoria = categoriasMap.get(catId);

                if (categoria == null) continue; // Si la categoría no existe, ignora esta colección

                var icono = Material.matchMaterial(colSec.getString(colKey + ".icono", "STONE").toUpperCase());
                if (icono == null) icono = Material.STONE;

                String nexoId = colSec.getString(colKey + ".nexo_id", "");
                String nombre = colSec.getString(colKey + ".nombre", "&#55FF55" + colKey);
                int slotMenu = colSec.getInt(colKey + ".slot_en_menu", 0);

                // 3. Leer los Tiers de esta colección
                Map<Integer, Tier> tiersMap = new HashMap<>();
                var tierSec = colSec.getConfigurationSection(colKey + ".tiers");

                if (tierSec != null) {
                    for (String tKey : tierSec.getKeys(false)) {
                        try {
                            int nivel = Integer.parseInt(tKey);
                            long requerido = tierSec.getLong(tKey + ".requerido", 100);
                            List<String> recompensas = tierSec.getStringList(tKey + ".recompensas");
                            List<String> lore = tierSec.getStringList(tKey + ".lore_recompensa");

                            tiersMap.put(nivel, new Tier(nivel, requerido, recompensas, lore));
                        } catch (NumberFormatException ignored) {
                            // Ignora claves que no sean números
                        }
                    }
                }

                // Ensamblamos el ítem y lo metemos a la categoría
                var item = new CollectionItem(colKey, catId, nombre, icono, nexoId, slotMenu, tiersMap);
                categoria.addItem(item);
            }
        }

        plugin.getLogger().info("✅ [COLECCIONES] Ensambladas " + categoriasMap.size() + " categorías de farmeo en RAM.");
        return categoriasMap;
    }

    // ==========================================================
    // 🔍 MÉTODOS DE COMPATIBILIDAD (Slayers / Consultas crudas)
    // ==========================================================

    public boolean esColeccion(String id) {
        return config.contains("colecciones." + id);
    }

    public ConfigurationSection getDatosColeccion(String id) {
        return config.getConfigurationSection("colecciones." + id);
    }

    public boolean esSlayer(String id) {
        return config.contains("slayers." + id);
    }

    public ConfigurationSection getDatosSlayer(String id) {
        return config.getConfigurationSection("slayers." + id);
    }
}
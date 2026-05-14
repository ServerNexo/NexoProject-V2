package me.aeroxis.factories.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.factories.AeroxisFactories;
import me.aeroxis.factories.core.CraftingRecipe;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 🏭 AeroxisFactories - Gestor Dinámico de Recetas (Arquitectura AAA)
 * Rendimiento: Caché en RAM, Búsquedas Funcionales y Cero Hardcoding.
 */
@Singleton
public class RecipeManager {

    private final AeroxisFactories plugin;
    
    // 🌟 CACHÉ O(1): Todas las recetas viven aquí para no leer el disco duro nunca más
    private final Map<String, CraftingRecipe> recipeCache = new ConcurrentHashMap<>();

    @Inject
    public RecipeManager(AeroxisFactories plugin) {
        this.plugin = plugin;
        loadRecipes();
    }

    public void loadRecipes() {
        recipeCache.clear();
        File file = new File(plugin.getDataFolder(), "recipes.yml");

        // Si el archivo no existe, creamos uno de ejemplo
        if (!file.exists()) {
            plugin.saveResource("recipes.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.getConfigurationSection("recetas") == null) return;

        for (String key : config.getConfigurationSection("recetas").getKeys(false)) {
            try {
                String path = "recetas." + key;
                String requiredNode = config.getString(path + ".nodo_requerido", "NONE");
                
                String resultData = config.getString(path + ".resultado");
                String[] resultSplit = resultData.split(":");
                String resultItemId = resultSplit[0] + ":" + resultSplit[1];
                int resultAmount = resultSplit.length > 2 ? Integer.parseInt(resultSplit[2]) : 1;

                Map<String, Integer> ingredients = new HashMap<>();
                for (String ing : config.getStringList(path + ".ingredientes")) {
                    String[] ingSplit = ing.split(":");
                    String ingId = ingSplit[0] + ":" + ingSplit[1];
                    int ingAmount = ingSplit.length > 2 ? Integer.parseInt(ingSplit[2]) : 1;
                    ingredients.put(ingId, ingAmount);
                }

                CraftingRecipe recipe = new CraftingRecipe(key, requiredNode, ingredients, resultItemId, resultAmount);
                recipeCache.put(key, recipe);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "❌ Error al cargar la receta: " + key, e);
            }
        }
        plugin.getLogger().info("✅ Se han cargado " + recipeCache.size() + " recetas industriales en memoria.");
    }

    /**
     * 🔍 Motor de Búsqueda: Revisa todas las recetas para ver cuál encaja con los ítems de la mesa.
     */
    public Optional<CraftingRecipe> findMatchingRecipe(Map<String, Integer> itemsOnTable) {
        // 🌟 JAVA 21 STREAMS: Filtrado ultra veloz en la RAM
        return recipeCache.values().stream()
                .filter(recipe -> recipe.matches(itemsOnTable))
                .findFirst();
    }

    public CraftingRecipe getRecipe(String id) {
        return recipeCache.get(id);
    }
}
package me.nexo.islas.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.NexoPasterService; // 🌟 IMPORT DEL CORE
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandDatabase;
import me.nexo.islas.data.IslandProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏝️ Gestor Central de Islas (Grid + Caché en RAM para TPS Perfectos)
 * Cero dependencias externas. Usa archivos .nbt nativos de PaperMC.
 */
@Singleton
public class IslandManager {

    private final NexoIslas plugin;
    private final NexoPasterService pasterService;
    private final IslandDatabase db;
    private World islandWorld;

    // 🌟 NUEVO: CACHÉ EN RAM DE ALTO RENDIMIENTO (Reemplaza las consultas a DB lentas)
    private final Map<Integer, IslandProfile> activeIslands = new ConcurrentHashMap<>();

    // Distancia entre isla e isla (1000 bloques de vacío)
    private static final int ISLAND_SPACING = 1000;

    @Inject
    public IslandManager(NexoIslas plugin, NexoPasterService pasterService, IslandDatabase db) {
        this.plugin = plugin;
        this.pasterService = pasterService;
        this.db = db;

        // Generamos el mundo de vacío general si no existe
        setupEmptyWorld();
    }

    private void setupEmptyWorld() {
        WorldCreator creator = new WorldCreator("nexo_islas_world");
        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"layers\": [], \"biome\":\"minecraft:the_void\"}");
        this.islandWorld = Bukkit.createWorld(creator);
    }

    // ==========================================
    // 🧮 MATEMÁTICA INVERSA (MAGIA DE ARQUITECTO)
    // Traduce una Coordenada X,Z al Perfil de la Isla al instante.
    // ==========================================
    public IslandProfile getIslandAt(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals("nexo_islas_world")) return null;

        // Redondeamos para saber a qué "Casilla" de la cuadrícula pertenece este bloque
        int gridX = (int) Math.round(loc.getX() / (double) ISLAND_SPACING);
        int gridZ = (int) Math.round(loc.getZ() / (double) ISLAND_SPACING);
        int gridIndex = (gridZ * 100) + gridX;

        return activeIslands.get(gridIndex); // O(1) Búsqueda instantánea en RAM
    }

    /**
     * 🌟 CREAR ISLA: Registra en la BD, obtiene el grid_index oficial, calcula coordenada y pega el .nbt
     */
    public void createIslandAsync(Player player) {
        player.sendMessage("§e⏳ Contactando a los Arquitectos celestiales...");

        // 1. Pedimos a PostgreSQL que cree el registro y nos dé un número de cuadrícula en orden
        db.createNewIsland(player.getUniqueId()).thenAccept(gridIndex -> {
            if (gridIndex == -1) {
                player.sendMessage("§c❌ Error crítico conectando con el Nexo (Base de Datos).");
                return;
            }

            // 2. Calculamos las coordenadas X, Z reales usando el gridIndex oficial
            int gridX = (gridIndex % 100) * ISLAND_SPACING;
            int gridZ = (gridIndex / 100) * ISLAND_SPACING;

            // Centramos la estructura en Y=100
            Location pasteLocation = new Location(islandWorld, gridX, 100, gridZ);

            // 3. Usamos el motor del Core para inyectar la estructura sin lag
            pasterService.pasteTemplateAsync("template_isla", pasteLocation).thenAccept(success -> {
                if (success) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§a✅ ¡Tu isla ha sido materializada en el sector #" + gridIndex + "!");
                        // 🌟 Al crear, cargamos el perfil a la RAM y lo teletransportamos
                        loadIslandAsync(player);
                    });
                } else {
                    player.sendMessage("§c❌ Error: No se encontró el archivo 'template_isla.nbt' en NexoCore/templates/");
                }
            });

        }).exceptionally(ex -> {
            plugin.getLogger().severe("❌ Error asíncrono creando isla nativa: " + ex.getMessage());
            return null;
        });
    }

    /**
     * 🌟 CARGAR ISLA (Login): Lee el grid_index desde PostgreSQL y teletransporta.
     */
    public void loadIslandAsync(Player player) {
        player.sendMessage("§e⏳ Localizando tu isla en los registros...");

        // Llamamos asíncronamente a PostgreSQL para traer su perfil
        db.loadIsland(player.getUniqueId()).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Si el perfil es null, nunca ejecutó /is create
                if (profile == null) {
                    player.sendMessage("§c❌ No tienes una isla. Usa /is create");
                    return;
                }

                // 🌟 GUARDAMOS EN RAM PARA LOS PERMISOS RÁPIDOS
                activeIslands.put(profile.getGridIndex(), profile);

                // Calculamos las coordenadas matemáticas basándonos en el grid_index
                int gridIndex = profile.getGridIndex();
                int gridX = (gridIndex % 100) * ISLAND_SPACING;
                int gridZ = (gridIndex / 100) * ISLAND_SPACING;

                Location islandLoc = new Location(islandWorld, gridX, 102, gridZ);
                player.teleport(islandLoc);
                player.sendMessage("§a✅ Volando de regreso a tu isla...");
            });
        });
    }

    /**
     * 💤 APAGAR ISLA (Hibernación)
     */
    public void unloadIslandSafe(String islandId) {
        // En el sistema de cuadrícula (Grid Nativo), no descargamos mundos de la RAM.
        // Aquí podemos en un futuro remover el perfil de `activeIslands` si la isla se queda 100% vacía para liberar RAM.
    }

    // 🌟 GETTER NECESARIO PARA LOS COMANDOS
    public NexoIslas getPlugin() {
        return plugin;
    }
}
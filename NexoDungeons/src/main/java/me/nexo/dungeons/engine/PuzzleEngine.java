package me.nexo.dungeons.engine;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.dungeons.NexoDungeons;
import me.nexo.dungeons.data.EventRule;
import org.bukkit.Location;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🏰 NexoDungeons - Motor Matemático de Puzzles y Eventos (Arquitectura Enterprise)
 * Rendimiento: Virtual Thread Executor (I/O No-Bloqueante), Búsqueda O(1) y Prevención de Leaks.
 */
@Singleton
public class PuzzleEngine {

    private final NexoDungeons plugin;
    private final Gson gson;

    // 🌟 JAVA 21: Motor de Hilos Virtuales para I/O (Lectura/Escritura JSON)
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ⚡ MAPA ULTRA RÁPIDO (O(1)): Llave = "NombreMundo_X_Y_Z"
    private final Map<String, EventRule> activeRules = new ConcurrentHashMap<>();

    // 📂 Plantillas de Instancias (Se guardan aquí y se inyectan al clonar la dungeon)
    private final Map<String, EventRule> instancedTemplates = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa
    @Inject
    public PuzzleEngine(NexoDungeons plugin) {
        this.plugin = plugin;
        this.gson = new Gson();

        // 🌟 FIX I/O: Prevenimos el bloqueo del Main Thread al arrancar el servidor
        ioExecutor.submit(() -> {
            crearArchivoEjemplo();
            loadRulesAsync();
        });
    }

    private void crearArchivoEjemplo() {
        try {
            var dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            var file = new File(dataFolder, "events.json");
            
            if (!file.exists()) {
                String jsonDefault = """
                {
                  "eventos": [
                    {
                      "id": "EJEMPLO_DRAGON_PUBLICO",
                      "isInstanced": false,
                      "worldName": "world_the_end",
                      "trigger": { "type": "PLAYER_INTERACT", "material": "DRAGON_EGG", "loc": {"x": 0, "y": 80, "z": 0} },
                      "actions": [
                        { "type": "SPAWN_MYTHICMOB", "mobId": "NexoDragon", "loc": {"x": 0, "y": 100, "z": 0} }
                      ]
                    }
                  ]
                }
                """;
                Files.writeString(file.toPath(), jsonDefault);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error creando archivo de ejemplo JSON: " + e.getMessage());
        }
    }

    public void loadRulesAsync() {
        // 🌟 Ejecutamos la lectura pesada asíncronamente
        ioExecutor.submit(() -> {
            try {
                var file = new File(plugin.getDataFolder(), "events.json");
                if (!file.exists()) return;

                // 🌟 FIX RESOURCE LEAK: Try-with-resources cierra el archivo automáticamente al terminar
                try (var reader = new FileReader(file)) {
                    var json = gson.fromJson(reader, JsonObject.class);
                    if (json == null || !json.has("eventos")) return;

                    var eventos = json.getAsJsonArray("eventos");
                    int cargados = 0;

                    // 🌟 PREVENCIÓN RAM: Limpiamos los mapas en caso de que sea un comando /dungeon reload
                    activeRules.clear();
                    instancedTemplates.clear();

                    for (JsonElement el : eventos) {
                        EventRule rule = gson.fromJson(el, EventRule.class);

                        if (rule.isInstanced()) {
                            instancedTemplates.put(rule.id(), rule);
                        } else {
                            // Eventos públicos (Coordenadas absolutas)
                            String locKey = rule.worldName() + "_" +
                                    rule.trigger().loc().get("x") + "_" +
                                    rule.trigger().loc().get("y") + "_" +
                                    rule.trigger().loc().get("z");
                            activeRules.put(locKey, rule);
                            cargados++;
                        }
                    }
                    plugin.getLogger().info("✅ [PUZZLE ENGINE] " + cargados + " eventos globales y " + instancedTemplates.size() + " plantillas de Dungeon cargadas en memoria RAM.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error leyendo events.json: " + e.getMessage());
            }
        });
    }

    // ⚡ MÉTODO CRÍTICO: Búsqueda O(1) para el DungeonListener
    public EventRule getRuleAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        String key = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        return activeRules.get(key);
    }

    // Cuando el DungeonGridManager pegue el schematic, llamará a esto para activar los puzzles
    // (Offset Injection)
    public void injectInstancedRules(String worldName, int offsetX, int offsetY, int offsetZ) {
        for (var template : instancedTemplates.values()) {
            int realX = template.trigger().loc().get("x") + offsetX;
            int realY = template.trigger().loc().get("y") + offsetY;
            int realZ = template.trigger().loc().get("z") + offsetZ;

            String key = worldName + "_" + realX + "_" + realY + "_" + realZ;
            activeRules.put(key, template); // Inyectamos el puzzle en la coordenada real clonada
        }
    }
}
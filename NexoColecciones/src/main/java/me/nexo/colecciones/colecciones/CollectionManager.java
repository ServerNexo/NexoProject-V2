package me.nexo.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.colecciones.data.CollectionCategory;
import me.nexo.colecciones.data.CollectionItem;
import me.nexo.colecciones.data.Tier;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.core.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 📚 NexoColecciones - Gestor de Farmeo y Base de Datos (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales Gestionados, EntitySchedulers y Cero Acoplamiento Estático.
 */
@Singleton
public class CollectionManager {

    private final NexoColecciones plugin;
    private final ColeccionesConfig coleccionesConfig;
    private final DatabaseManager db;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia inyectada
    private final Gson gson;

    // 🌟 MOTOR ENTERPRISE: Pool de Hilos Virtuales Gestionado
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ⚡ MAPAS EN RAM: Velocidad de acceso O(1)
    private Map<String, CollectionCategory> categoriasRegistradas = new HashMap<>();
    private final Map<UUID, CollectionProfile> perfilesJugadores = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias
    @Inject
    public CollectionManager(NexoColecciones plugin, ColeccionesConfig coleccionesConfig, DatabaseManager db, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.coleccionesConfig = coleccionesConfig;
        this.db = db;
        this.crossplayUtils = crossplayUtils;
        this.gson = new Gson();
    }

    public void cargarDesdeConfig() {
        this.categoriasRegistradas = coleccionesConfig.cargarCategoriasEnRam();
    }

    // 📥 CARGA DE DATOS ASÍNCRONA (VIRTUAL THREADS GESTIONADOS)
    public void loadPlayerFromDatabase(UUID uuid) {
        virtualExecutor.submit(() -> {
            String sql = "SELECT collections_data, claimed_tiers FROM nexo_collections WHERE uuid = ?";
            try (var conn = db.getConnection(); var ps = conn.prepareStatement(sql)) {
                
                ps.setObject(1, uuid); // 🌟 FIX: Tipado UUID nativo para SQL moderno
                var rs = ps.executeQuery();

                if (rs.next()) {
                    String jsonProgress = rs.getString("collections_data");
                    String jsonClaimed = rs.getString("claimed_tiers");

                    Map<String, Integer> mapProgress = gson.fromJson(jsonProgress, new TypeToken<Map<String, Integer>>() {}.getType());
                    Map<String, Set<Integer>> mapClaimed = gson.fromJson(jsonClaimed, new TypeToken<Map<String, Set<Integer>>>() {}.getType());

                    if (mapProgress == null) mapProgress = new HashMap<>();
                    if (mapClaimed == null) mapClaimed = new HashMap<>();

                    perfilesJugadores.put(uuid, new CollectionProfile(uuid, mapProgress, mapClaimed));
                } else {
                    perfilesJugadores.put(uuid, new CollectionProfile(uuid, new HashMap<>(), new HashMap<>()));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando perfil de colección para " + uuid + ": " + e.getMessage());
                perfilesJugadores.put(uuid, new CollectionProfile(uuid, new HashMap<>(), new HashMap<>())); // Fallback seguro
            }
        });
    }

    // 📈 PROGRESIÓN EN TIEMPO REAL (Llamado constantemente desde el Listener o Minions)
    public void addProgress(Player player, String itemId, int amount) {
        itemId = itemId.toLowerCase();
        var item = getItemGlobal(itemId);
        if (item == null) return;

        var profile = perfilesJugadores.get(player.getUniqueId());
        if (profile == null) return;

        int nivelViejo = calcularNivel(item, profile.getProgress(itemId));
        profile.addProgress(itemId, amount);
        int nivelNuevo = calcularNivel(item, profile.getProgress(itemId));

        // 🎉 SUBIDA DE NIVEL
        if (nivelNuevo > nivelViejo) {
            // PAPER FIX CRÍTICO: EntityScheduler para saltar al Hilo Principal y generar físicas/sonidos
            player.getScheduler().run(plugin, task -> {
                crossplayUtils.sendTitle(player,
                        "&#FFAA00<bold>NIVEL " + nivelNuevo + "</bold>",
                        "&#E6CCFF" + item.getNombre());

                crossplayUtils.sendMessage(player, "&#555555--------------------------------");
                crossplayUtils.sendMessage(player, "&#FFAA00🌟 <bold>COLECCIÓN MEJORADA</bold>");
                crossplayUtils.sendMessage(player, "&#E6CCFFHas alcanzado el nivel &#55FF55" + nivelNuevo + " &#E6CCFFen &#55FF55" + item.getNombre());
                crossplayUtils.sendMessage(player, "&#555555--------------------------------");

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

                // Anuncio Global si alcanzó la maestría
                if (nivelNuevo == item.getMaxTier()) {
                    crossplayUtils.broadcastMessage(" ");
                    crossplayUtils.broadcastMessage("&#ff00ff🏆 <bold>¡MAESTRÍA ALCANZADA!</bold> &#E6CCFF" + player.getName() + " ha maximizado la colección de &#55FF55" + item.getNombre() + "&#E6CCFF.");
                    crossplayUtils.broadcastMessage(" ");
                }
            }, null);
        }
    }

    public int calcularNivel(CollectionItem item, int cantidadFarmeada) {
        int nivelAlcanzado = 0;
        List<Integer> niveles = new ArrayList<>(item.getTiers().keySet());
        Collections.sort(niveles); // Ordenamos de menor a mayor
        
        for (int nivel : niveles) {
            Tier tier = item.getTier(nivel);
            if (cantidadFarmeada >= tier.getRequerido()) {
                nivelAlcanzado = nivel;
            } else {
                break;
            }
        }
        return nivelAlcanzado;
    }

    // 🎁 RECOMPENSAS
    public void reclamarRecompensa(Player player, String itemId, int targetTier) {
        var profile = perfilesJugadores.get(player.getUniqueId());
        if (profile == null) return;

        var item = getItemGlobal(itemId);
        if (item == null) return;

        var tier = item.getTier(targetTier);
        if (tier == null) return;

        if (profile.getProgress(itemId) < tier.getRequerido()) return;
        if (profile.hasClaimedTier(itemId, targetTier)) return;

        profile.markTierAsClaimed(itemId, targetTier);

        // PAPER FIX CRÍTICO: Spawnear partículas requiere el Hilo Principal
        player.getScheduler().run(plugin, task -> {
            ejecutarRecompensas(player, tier.getRecompensas());

            crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>RECOMPENSA:</bold> &#E6CCFFHas reclamado los objetos de este nivel.");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.1);
        }, null);
    }

    private void ejecutarRecompensas(Player player, List<String> acciones) {
        // MAIN THREAD: Bukkit.dispatchCommand siempre debe ser síncrono.
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String accion : acciones) {
                String pName = player.getName();
                if (accion.startsWith("[comando] ")) {
                    String cmd = accion.replace("[comando] ", "").replace("{player}", pName).trim();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                } else if (accion.startsWith("[permiso] ")) {
                    String perm = accion.replace("[permiso] ", "").replace("{player}", pName).trim();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pName + " permission set " + perm + " true");
                }
            }
        });
    }

    // 🏆 TABLA DE LÍDERES ASÍNCRONA (SQL JSONB + Virtual Threads)
    public void calcularTopAsync(Player player, String itemId) {
        var cItem = getItemGlobal(itemId);
        if (cItem == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Esta colección no existe o está deshabilitada.");
            return;
        }

        virtualExecutor.submit(() -> {
            // DB inyectada directamente para consultas JSONB nativas
            String sql = "SELECT j.name, CAST(c.collections_data->>? AS INTEGER) as amount " +
                    "FROM nexo_collections c " +
                    "JOIN jugadores j ON c.uuid = j.uuid " +
                    "WHERE c.collections_data ? ? " +
                    "ORDER BY amount DESC LIMIT 5";

            try (var conn = db.getConnection();
                 var ps = conn.prepareStatement(sql)) {

                ps.setString(1, cItem.getId());
                ps.setString(2, cItem.getId());
                var rs = ps.executeQuery();

                List<String> lineasTop = new ArrayList<>();
                int rank = 1;
                while (rs.next()) {
                    String pName = rs.getString("name");
                    int amt = rs.getInt("amount");
                    lineasTop.add("&#E6CCFF" + rank + ". &#55FF55" + pName + " &#555555- &#FFAA00" + amt);
                    rank++;
                }

                // Envío directo asíncrono (Los mensajes son Thread-Safe)
                crossplayUtils.sendMessage(player, "&#555555--------------------------------");
                crossplayUtils.sendMessage(player, "&#FFAA00🏆 <bold>TOP 5: " + cItem.getNombre().toUpperCase() + "</bold>");
                if (lineasTop.isEmpty()) {
                    crossplayUtils.sendMessage(player, "&#FF5555Aún no hay registros en esta colección.");
                } else {
                    lineasTop.forEach(l -> crossplayUtils.sendMessage(player, l));
                }
                crossplayUtils.sendMessage(player, "&#555555--------------------------------");

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error calculando Top de " + itemId + ": " + e.getMessage());
                crossplayUtils.sendMessage(player, "&#8b0000[!] Error crítico de red al contactar con la base de datos.");
            }
        });
    }

    // ==========================================
    // 🔍 UTILIDADES DE BÚSQUEDA
    // ==========================================
    
    public CollectionItem getItemGlobal(String itemId) {
        for (CollectionCategory cat : categoriasRegistradas.values()) {
            if (cat.getItems().containsKey(itemId.toLowerCase())) {
                return cat.getItems().get(itemId.toLowerCase());
            }
        }
        return null;
    }

    public Map<String, CollectionCategory> getCategorias() { 
        return categoriasRegistradas; 
    }
    
    public CollectionProfile getProfile(UUID uuid) { 
        return perfilesJugadores.get(uuid); 
    }
    
    public void removeProfile(UUID uuid) { 
        perfilesJugadores.remove(uuid); 
    }
    
    public Map<UUID, CollectionProfile> getPerfiles() { 
        return perfilesJugadores; 
    }
}
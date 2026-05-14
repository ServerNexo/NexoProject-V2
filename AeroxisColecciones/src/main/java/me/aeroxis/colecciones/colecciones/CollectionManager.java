package me.aeroxis.colecciones.colecciones;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.colecciones.AeroxisColecciones;
import me.aeroxis.colecciones.data.CollectionCategory;
import me.aeroxis.colecciones.data.CollectionItem;
import me.aeroxis.colecciones.data.RewardTemplate;
import me.aeroxis.colecciones.data.Tier;
import me.aeroxis.core.api.AeroxisColeccionesAPI;
import me.aeroxis.core.api.ServiceManager;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.core.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 📚 AeroxisColecciones - Gestor de Farmeo y Base de Datos (Arquitectura Enterprise)
 * Rendimiento: Hilos Virtuales Gestionados, EntitySchedulers y API Global.
 */
@Singleton
public class CollectionManager implements AeroxisColeccionesAPI {

    private final AeroxisColecciones plugin;
    private final ColeccionesConfig coleccionesConfig;
    private final DatabaseManager db;
    private final CrossplayUtils crossplayUtils;
    private final Gson gson;

    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private Map<String, CollectionCategory> categoriasRegistradas = new HashMap<>();
    private final Map<UUID, CollectionProfile> perfilesJugadores = new ConcurrentHashMap<>();

    // 🌟 MÓDULO 1: CACHÉ DE PLANTILLAS DE RECOMPENSAS
    private final Map<String, RewardTemplate> rewardTemplates = new ConcurrentHashMap<>();

    @Inject
    public CollectionManager(AeroxisColecciones plugin, ColeccionesConfig coleccionesConfig, DatabaseManager db, CrossplayUtils crossplayUtils, ServiceManager serviceManager) {
        this.plugin = plugin;
        this.coleccionesConfig = coleccionesConfig;
        this.db = db;
        this.crossplayUtils = crossplayUtils;
        this.gson = new Gson();

        serviceManager.register(AeroxisColeccionesAPI.class, this);
    }

    // ==========================================
    // 🌟 MÓDULO 1: GESTIÓN DE PLANTILLAS
    // ==========================================

    public void loadRewardTemplates(org.bukkit.configuration.file.FileConfiguration config) {
        rewardTemplates.clear();

        if (config == null || !config.contains("templates")) {
            plugin.getLogger().warning("⚠️ No se encontraron plantillas (templates) en el archivo de configuración.");
            return;
        }

        for (String key : config.getConfigurationSection("templates").getKeys(false)) {
            List<String> lore = config.getStringList("templates." + key + ".lore");
            List<String> comandos = config.getStringList("templates." + key + ".comandos");

            rewardTemplates.put(key, new RewardTemplate(key, lore, comandos));
        }

        plugin.getLogger().info("✅ Se cargaron " + rewardTemplates.size() + " plantillas de recompensas.");
    }

    public RewardTemplate getRewardTemplate(String id) {
        return rewardTemplates.get(id);
    }

    // ==========================================
    // 💾 CARGA Y BASE DE DATOS
    // ==========================================

    public void cargarDesdeConfig() {
        this.categoriasRegistradas = coleccionesConfig.cargarCategoriasEnRam();
    }

    public void loadPlayerFromDatabase(UUID uuid) {
        virtualExecutor.submit(() -> {
            String sql = "SELECT collections_data, claimed_tiers FROM nexo_collections WHERE uuid = ?";
            try (var conn = db.getConnection(); var ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
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
                perfilesJugadores.put(uuid, new CollectionProfile(uuid, new HashMap<>(), new HashMap<>()));
            }
        });
    }

    // ==========================================
    // 🌐 MÉTODOS DE LA API GLOBAL (AeroxisCore)
    // ==========================================

    @Override
    public long getCollectionAmount(UUID playerId, String collectionId) {
        CollectionProfile profile = perfilesJugadores.get(playerId);
        if (profile == null) return 0L;
        return profile.getProgress(collectionId.toLowerCase());
    }

    @Override
    public void addCollectionProgress(UUID playerId, String collectionId, int amount) {
        String itemId = collectionId.toLowerCase();
        var item = getItemGlobal(itemId);
        if (item == null) return;

        var profile = perfilesJugadores.get(playerId);
        if (profile == null) return;

        int nivelViejo = calcularNivel(item, profile.getProgress(itemId));
        profile.addProgress(itemId, amount);
        int nivelNuevo = calcularNivel(item, profile.getProgress(itemId));

        // 🎉 SUBIDA DE NIVEL
        if (nivelNuevo > nivelViejo) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.getScheduler().run(plugin, task -> {
                    crossplayUtils.sendTitle(player,
                            "&#FFAA00<bold>NIVEL " + nivelNuevo + "</bold>",
                            "&#E6CCFF" + item.getNombre());

                    crossplayUtils.sendMessage(player, "&#555555--------------------------------");
                    crossplayUtils.sendMessage(player, "&#FFAA00🌟 <bold>COLECCIÓN MEJORADA</bold>");
                    crossplayUtils.sendMessage(player, "&#E6CCFFHas alcanzado el nivel &#55FF55" + nivelNuevo + " &#E6CCFFen &#55FF55" + item.getNombre());
                    crossplayUtils.sendMessage(player, "&#555555--------------------------------");

                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

                    if (nivelNuevo == item.getMaxTier()) {
                        crossplayUtils.broadcastMessage(" ");
                        crossplayUtils.broadcastMessage("&#ff00ff🏆 <bold>¡MAESTRÍA ALCANZADA!</bold> &#E6CCFF" + player.getName() + " ha maximizado la colección de &#55FF55" + item.getNombre() + "&#E6CCFF.");
                        crossplayUtils.broadcastMessage(" ");
                    }
                }, null);
            }
        }
    }

    // ==========================================
    // ⭐ MÓDULO 3: EL MOTOR DE EJECUCIÓN (Loot y Seguridad)
    // ==========================================

    public int calcularNivel(CollectionItem item, int cantidadFarmeada) {
        int nivelAlcanzado = 0;
        List<Integer> niveles = new ArrayList<>(item.getTiers().keySet());
        Collections.sort(niveles);

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

    public void reclamarRecompensa(Player player, String itemId, int targetTier) {
        var profile = perfilesJugadores.get(player.getUniqueId());
        if (profile == null) return;

        var item = getItemGlobal(itemId);
        if (item == null) return;

        var tier = item.getTier(targetTier);
        if (tier == null) return;

        // 🛡️ 1. Validaciones de Seguridad Estrictas (Anti-Dupeo)
        if (profile.getProgress(itemId) < tier.getRequerido()) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Aún no tienes el progreso necesario.");
            return;
        }

        if (profile.hasClaimedTier(itemId, targetTier)) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Ya has reclamado esta recompensa.");
            return;
        }

        // 🎁 2. Obtener Plantilla de Recompensa
        RewardTemplate template = getRewardTemplate(tier.getRecompensaId());
        if (template == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Error interno: No se encontró la plantilla de recompensa. Reporta esto al staff.");
            return;
        }

        // 💾 3. Guardado en RAM y Base de Datos (Asíncrono = 0 Lag)
        profile.markTierAsClaimed(itemId, targetTier);

        virtualExecutor.submit(() -> {
            String sql = "UPDATE nexo_collections SET claimed_tiers = ?::jsonb WHERE uuid = ?";
            try (var conn = db.getConnection(); var ps = conn.prepareStatement(sql)) {
                // Dependiendo de cómo se llame el getter en tu CollectionProfile,
                // asumo que tienes un getClaimedTiersMap() o puedes crearlo.
                // Usamos la serialización GSON para guardar directamente el JSON actualizado en la BD
                ps.setString(1, gson.toJson(profile.getClaimedTiersMap()));
                ps.setString(2, player.getUniqueId().toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error SQL guardando recompensa asíncrona de " + player.getName() + ": " + e.getMessage());
            }
        });

        // ⚡ 4. Despacho de Loot y Feedback (En el Hilo Principal de Bukkit)
        player.getScheduler().run(plugin, task -> {

            // Ejecutamos los comandos de la plantilla
            ejecutarRecompensas(player, template.comandos());

            // Feedback Cinematográfico
            crossplayUtils.sendMessage(player, "&#55FF55[✓] <bold>RECOMPENSA:</bold> &#E6CCFFHas reclamado los objetos de este nivel.");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.1);

        }, null);
    }

    private void ejecutarRecompensas(Player player, List<String> comandos) {
        // No es necesario usar runTask aquí porque ya estamos dentro de player.getScheduler().run(...)
        for (String cmd : comandos) {
            String pName = player.getName();

            // Reemplazo de variables universales (%player% es el nuevo estándar, {player} el antiguo)
            String parsedCmd = cmd.replace("%player%", pName).replace("{player}", pName).trim();

            // Soporte de compatibilidad hacia atrás
            if (parsedCmd.startsWith("[comando] ")) {
                parsedCmd = parsedCmd.replace("[comando] ", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
            } else if (parsedCmd.startsWith("[permiso] ")) {
                String perm = parsedCmd.replace("[permiso] ", "");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pName + " permission set " + perm + " true");
            } else {
                // Formato limpio sin prefijos
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
            }
        }
    }

    // ==========================================
    // 📊 CÁLCULOS Y RANKINGS
    // ==========================================

    public void calcularTopAsync(Player player, String itemId) {
        var cItem = getItemGlobal(itemId);
        if (cItem == null) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] Esta colección no existe o está deshabilitada.");
            return;
        }

        virtualExecutor.submit(() -> {
            // 🌟 FIX APLICADO: Extracción JSONB segura para JDBC (IS NOT NULL)
            String sql = "SELECT j.name, CAST(c.collections_data->>? AS INTEGER) as amount " +
                    "FROM nexo_collections c " +
                    "JOIN jugadores j ON c.uuid = j.uuid " +
                    "WHERE c.collections_data->>? IS NOT NULL " +
                    "ORDER BY amount DESC LIMIT 5";

            try (var conn = db.getConnection(); var ps = conn.prepareStatement(sql)) {
                // Ahora solo hay 2 signos de interrogación '?' en el SQL
                ps.setString(1, cItem.getId()); // Inyecta el ID en el SELECT
                ps.setString(2, cItem.getId()); // Inyecta el ID en el WHERE

                var rs = ps.executeQuery();

                List<String> lineasTop = new ArrayList<>();
                int rank = 1;
                while (rs.next()) {
                    String pName = rs.getString("name");
                    int amt = rs.getInt("amount");
                    lineasTop.add("&#E6CCFF" + rank + ". &#55FF55" + pName + " &#555555- &#FFAA00" + amt);
                    rank++;
                }

                crossplayUtils.sendMessage(player, "&#555555--------------------------------");
                crossplayUtils.sendMessage(player, "&#FFAA00🏆 <bold>TOP 5: " + cItem.getNombre().toUpperCase() + "</bold>");
                if (lineasTop.isEmpty()) crossplayUtils.sendMessage(player, "&#FF5555Aún no hay registros en esta colección.");
                else lineasTop.forEach(l -> crossplayUtils.sendMessage(player, l));
                crossplayUtils.sendMessage(player, "&#555555--------------------------------");

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error calculando Top de " + itemId + ": " + e.getMessage());
                crossplayUtils.sendMessage(player, "&#8b0000[!] Error crítico de red al contactar con la base de datos.");
            }
        });
    }

    public int getItemPDCValue(String itemId) {
        CollectionItem item = getItemGlobal(itemId);
        return item != null ? 1 : 0;
    }

    public CollectionItem getItemGlobal(String itemId) {
        for (CollectionCategory cat : categoriasRegistradas.values()) {
            if (cat.getItems().containsKey(itemId.toLowerCase())) {
                return cat.getItems().get(itemId.toLowerCase());
            }
        }
        return null;
    }

    public Map<String, CollectionCategory> getCategorias() { return categoriasRegistradas; }
    public CollectionProfile getProfile(UUID uuid) { return perfilesJugadores.get(uuid); }
    public void removeProfile(UUID uuid) { perfilesJugadores.remove(uuid); }
    public Map<UUID, CollectionProfile> getPerfiles() { return perfilesJugadores; }
}
package me.aeroxis.colecciones.slayers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.colecciones.AeroxisColecciones;
import me.aeroxis.core.crossplay.CrossplayUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 📚 AeroxisColecciones - Gestor Central de Cacerías (Arquitectura Enterprise)
 * Rendimiento: Carga de Archivos O(1) Asíncrona, Fallbacks Dinámicos y Compatibilidad con Menús.
 */
@Singleton
public class SlayerManager {

    private final AeroxisColecciones plugin;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia Inyectada

    // 🌟 Gestor formal de Hilos Virtuales para I/O Masivo (Archivos YAML)
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // DTO Inmutable
    public record SlayerTemplate(String id, String name, String targetMob, int requiredKills, String bossName, String bossType) {}

    // Mapas 100% Concurrentes
    private final Map<String, SlayerTemplate> templates = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveSlayer> activeSlayers = new ConcurrentHashMap<>();

    // 💉 Inyección de Dependencias Directa
    @Inject
    public SlayerManager(AeroxisColecciones plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 Carga de datos inicial a la RAM ejecutada fuera del Main Thread
    public void cargarSlayers() {
        virtualExecutor.submit(() -> {
            templates.clear();
            var file = new File(plugin.getDataFolder(), "slayers.yml");

            if (!file.exists()) {
                try {
                    plugin.saveResource("slayers.yml", false);
                } catch (Exception e) {
                    plugin.getLogger().warning("⚠️ No se pudo guardar slayers.yml por defecto.");
                }
            }

            var config = YamlConfiguration.loadConfiguration(file);
            int count = 0;

            for (String key : config.getKeys(false)) {
                String name = config.getString(key + ".nombre", key);
                String targetMob = config.getString(key + ".mob_objetivo", "ZOMBIE");
                int kills = config.getInt(key + ".kills_necesarias", 100);
                String bossName = config.getString(key + ".boss_nombre", "Boss");
                String bossType = config.getString(key + ".boss_tipo", "ZOMBIE");

                templates.put(key.toUpperCase(), new SlayerTemplate(key.toUpperCase(), name, targetMob, kills, bossName, bossType));
                count++;
            }

            plugin.getLogger().info("✅ [SLAYER MANAGER] Ensamblados " + count + " contratos Slayer en RAM.");
        });
    }

    public Map<String, SlayerTemplate> getTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    public ActiveSlayer getActiveSlayer(UUID uuid) { return activeSlayers.get(uuid); }
    public void removeActiveSlayer(UUID uuid) { activeSlayers.remove(uuid); }

    // ==========================================
    // ⚔️ CONTROLADORES PARA EL MENÚ (SlayerMenu.java)
    // ==========================================

    /**
     * Verifica en O(1) si un jugador ya tiene un contrato activo.
     */
    public boolean hasActiveQuest(UUID uuid) {
        return activeSlayers.containsKey(uuid);
    }

    /**
     * Inicia una misión de caza. Incluye Auto-Fallback si el Jefe no está en el slayers.yml
     */
    public void startQuest(UUID playerId, String bossId, String fallbackMob, int fallbackKills) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        var idUpper = bossId.toUpperCase();
        SlayerTemplate template = templates.get(idUpper);

        // 🌟 AUTO-RECOVERY: Si alguien borra el archivo YAML por accidente,
        // el código usa los datos seguros enviados desde el menú.
        if (template == null) {
            String bossName = idUpper.replace("_BOSS", "").replace("_", " ");
            template = new SlayerTemplate(idUpper, bossName, fallbackMob, fallbackKills, bossName, fallbackMob);
            templates.put(idUpper, template); // Lo registramos temporalmente
        }

        // Instanciamos el rastreador en memoria
        var activo = new ActiveSlayer(player, template);
        activeSlayers.put(playerId, activo);

        crossplayUtils.sendMessage(player, "&#555555--------------------------------");
        crossplayUtils.sendMessage(player, "&#FF5555⚔ <bold>NUEVA CACERÍA INICIADA</bold>");
        crossplayUtils.sendMessage(player, "&#E6CCFFHas firmado un contrato de sangre para aniquilar a &#FF5555" + template.name() + "&#E6CCFF.");
        crossplayUtils.sendMessage(player, "&#E6CCFFObjetivo inicial: Derrota &#FFAA00" + template.requiredKills() + " " + template.targetMob() + "s &#E6CCFFpara invocar a la bestia.");
        crossplayUtils.sendMessage(player, "&#555555--------------------------------");
    }
}
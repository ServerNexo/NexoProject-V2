package me.nexo.colecciones.slayers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.crossplay.CrossplayUtils;
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
 * 📚 NexoColecciones - Gestor Central de Cacerías (Arquitectura Enterprise)
 * Rendimiento: Carga de Archivos O(1) Asíncrona (Hilos Virtuales) y Dependencias Inyectadas.
 */
@Singleton
public class SlayerManager {

    private final NexoColecciones plugin;
    private final CrossplayUtils crossplayUtils; // 🌟 Sinergia Inyectada

    // 🌟 FIX: Gestor formal de Hilos Virtuales para I/O Masivo (Archivos YAML)
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // DTO Inmutable (Excelente uso de Records Java 16+)
    public record SlayerTemplate(String id, String name, String targetMob, int requiredKills, String bossName, String bossType) {}

    // Mapas 100% Concurrentes para evitar crashes de lectura/escritura asíncrona
    private final Map<String, SlayerTemplate> templates = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveSlayer> activeSlayers = new ConcurrentHashMap<>();

    // 💉 PILAR 1: Inyección de Dependencias Directa (Cero acoplamiento estático)
    @Inject
    public SlayerManager(NexoColecciones plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    // 🌟 FIX: Carga de datos inicial a la RAM ejecutada fuera del Main Thread
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

    // Sellamos el mapa para que nadie pueda añadir/borrar jefes por error desde otro lado
    public Map<String, SlayerTemplate> getTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    public ActiveSlayer getActiveSlayer(UUID uuid) { return activeSlayers.get(uuid); }
    public void removeActiveSlayer(UUID uuid) { activeSlayers.remove(uuid); }

    public void iniciarSlayer(Player player, String slayerId) {
        var idUpper = slayerId.toUpperCase();

        if (!templates.containsKey(idUpper)) {
            crossplayUtils.sendMessage(player, "&#FF5555[!] El contrato especificado no existe o la tinta se ha borrado.");
            return;
        }

        if (activeSlayers.containsKey(player.getUniqueId())) {
            crossplayUtils.sendMessage(player, "&#FFAA00[!] Ya tienes una cacería activa. Termina o cancela tu contrato actual (/slayer cancel).");
            return;
        }

        var template = templates.get(idUpper);
        var activo = new ActiveSlayer(player, template);
        activeSlayers.put(player.getUniqueId(), activo);

        // 🌟 FIX: Textos Hexadecimales inyectados limpiamente
        crossplayUtils.sendMessage(player, "&#555555--------------------------------");
        crossplayUtils.sendMessage(player, "&#FF5555⚔ <bold>NUEVA CACERÍA INICIADA</bold>");
        crossplayUtils.sendMessage(player, "&#E6CCFFHas firmado un contrato de sangre para aniquilar a &#FF5555" + template.name() + "&#E6CCFF.");
        crossplayUtils.sendMessage(player, "&#E6CCFFObjetivo inicial: Derrota &#FFAA00" + template.requiredKills() + " " + template.targetMob() + "s &#E6CCFFpara invocar a la bestia.");
        crossplayUtils.sendMessage(player, "&#555555--------------------------------");
    }
}
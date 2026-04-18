package me.nexo.colecciones.slayers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.colecciones.NexoColecciones;
import me.nexo.core.crossplay.CrossplayUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📚 NexoColecciones - Gestor Central de Cacerías (Arquitectura Enterprise)
 */
@Singleton
public class SlayerManager {

    private final NexoColecciones plugin;

    // DTO Inmutable (Excelente uso de Records)
    public record SlayerTemplate(String id, String name, String targetMob, int requiredKills, String bossName, String bossType) {}

    // 🌟 FIX: Mapas 100% Concurrentes para evitar crashes de lectura/escritura asíncrona
    private final Map<String, SlayerTemplate> templates = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveSlayer> activeSlayers = new ConcurrentHashMap<>();

    // 💉 PILAR 3: Inyección de Dependencias Directa (Cero acoplamiento estático)
    @Inject
    public SlayerManager(NexoColecciones plugin) {
        this.plugin = plugin;
    }

    // Carga de datos inicial a la RAM (Ocurre solo en arranques o reloads)
    public void cargarSlayers() {
        templates.clear();
        File file = new File(plugin.getDataFolder(), "slayers.yml");
        if (!file.exists()) plugin.saveResource("slayers.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            String name = config.getString(key + ".nombre", key);
            String targetMob = config.getString(key + ".mob_objetivo", "ZOMBIE");
            int kills = config.getInt(key + ".kills_necesarias", 100);
            String bossName = config.getString(key + ".boss_nombre", "Boss");
            String bossType = config.getString(key + ".boss_tipo", "ZOMBIE");

            templates.put(key.toUpperCase(), new SlayerTemplate(key.toUpperCase(), name, targetMob, kills, bossName, bossType));
        }

        plugin.getLogger().info("✅ [SLAYER MANAGER] Ensamblados " + templates.size() + " contratos Slayer en RAM.");
    }

    // 🌟 FIX: Sellamos el mapa para que nadie pueda añadir/borrar jefes por error desde otro lado
    public Map<String, SlayerTemplate> getTemplates() {
        return Collections.unmodifiableMap(templates);
    }

    public ActiveSlayer getActiveSlayer(UUID uuid) { return activeSlayers.get(uuid); }
    public void removeActiveSlayer(UUID uuid) { activeSlayers.remove(uuid); }

    public void iniciarSlayer(Player player, String slayerId) {
        slayerId = slayerId.toUpperCase();

        if (!templates.containsKey(slayerId)) {
            CrossplayUtils.sendMessage(player, "&#FF5555[!] El contrato especificado no existe o la tinta se ha borrado.");
            return;
        }

        if (activeSlayers.containsKey(player.getUniqueId())) {
            CrossplayUtils.sendMessage(player, "&#FFAA00[!] Ya tienes una cacería activa. Termina o cancela tu contrato actual (/slayer cancel).");
            return;
        }

        SlayerTemplate template = templates.get(slayerId);
        ActiveSlayer activo = new ActiveSlayer(player, template);
        activeSlayers.put(player.getUniqueId(), activo);

        // 🌟 FIX: Textos Hexadecimales directos (0% Lag I/O)
        CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
        CrossplayUtils.sendMessage(player, "&#FF5555⚔ <bold>NUEVA CACERÍA INICIADA</bold>");
        CrossplayUtils.sendMessage(player, "&#E6CCFFHas firmado un contrato de sangre para aniquilar a &#FF5555" + template.name() + "&#E6CCFF.");
        CrossplayUtils.sendMessage(player, "&#E6CCFFObjetivo inicial: Derrota &#FFAA00" + template.requiredKills() + " " + template.targetMob() + "s &#E6CCFFpara invocar a la bestia.");
        CrossplayUtils.sendMessage(player, "&#555555--------------------------------");
    }
}
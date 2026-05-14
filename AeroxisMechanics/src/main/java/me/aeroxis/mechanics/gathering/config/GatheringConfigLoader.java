package me.aeroxis.mechanics.gathering.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.mechanics.AeroxisMechanics;
import me.aeroxis.mechanics.gathering.data.Profession;
import me.aeroxis.mechanics.gathering.world.GatheringZone;
import me.aeroxis.mechanics.gathering.world.ZoneManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;

/**
 * 📂 Lee el config.yml e inyecta las zonas físicas en el ZoneManager.
 */
@Singleton
public class GatheringConfigLoader {

    private final AeroxisMechanics plugin;
    private final ZoneManager zoneManager;

    @Inject
    public GatheringConfigLoader(AeroxisMechanics plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
    }

    public void loadZones() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("nexo_gathering.zonas");
        if (section == null) {
            plugin.getLogger().warning("⚠️ No se encontraron zonas de NexoGathering en la configuración.");
            return;
        }

        int count = 0;
        for (String zoneId : section.getKeys(false)) {
            try {
                ConfigurationSection zs = section.getConfigurationSection(zoneId);
                
                Profession profession = Profession.valueOf(zs.getString("profesion", "MINING").toUpperCase());
                String worldName = zs.getString("mundo", "world");
                Material depletedMat = Material.valueOf(zs.getString("material_agotado", "BEDROCK").toUpperCase());
                int regenTicks = zs.getInt("regeneracion_ticks", 1200);

                // Cargar el BoundingBox
                double minX = zs.getDouble("area.min_x");
                double minY = zs.getDouble("area.min_y");
                double minZ = zs.getDouble("area.min_z");
                double maxX = zs.getDouble("area.max_x");
                double maxY = zs.getDouble("area.max_y");
                double maxZ = zs.getDouble("area.max_z");
                BoundingBox box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

                // Crear el Record y registrarlo en el motor espacial
                GatheringZone zone = new GatheringZone(zoneId, profession, box, worldName, depletedMat, regenTicks);
                zoneManager.registerZone(zone);
                count++;

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error cargando la zona de Gathering '" + zoneId + "': " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("🗺️ NexoGathering: " + count + " zonas cargadas y registradas en el mapa.");
    }
}
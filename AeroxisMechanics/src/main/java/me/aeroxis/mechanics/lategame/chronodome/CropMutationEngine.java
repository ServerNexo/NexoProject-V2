package me.aeroxis.mechanics.lategame.chronodome;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.mechanics.AeroxisMechanics;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 🧬 Motor de Mutación de Cultivos (Lazy Evaluation)
 * Transforma el botín basándose en el clima milisegundos antes de caer.
 */
@Singleton
public class CropMutationEngine implements Listener {

    public enum DomeWeather { TOXIC_RAIN, ABSOLUTE_ZERO, SOLAR_FLARE, NORMAL }
    
    private DomeWeather currentWeather = DomeWeather.ABSOLUTE_ZERO; // Esto rotaría asíncronamente

    @Inject
    public CropMutationEngine(AeroxisMechanics plugin) {
        // En el futuro inyectaremos el WeatherManager aquí
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropHarvest(BlockBreakEvent event) {
        if (!event.getBlock().getWorld().getName().equals("chrono_dome")) return;

        Material type = event.getBlock().getType();
        
        // Ejemplo: Mutación de Melones en clima de Cero Absoluto
        if (type == Material.MELON && currentWeather == DomeWeather.ABSOLUTE_ZERO) {
            event.setDropItems(false); // Cancelamos el drop vanilla
            
            // 💡 Oraxen/AeroxisItems API: Aquí darías el "Melón Glaciar"
            ItemStack mutatedCrop = new ItemStack(Material.PRISMARINE_CRYSTALS); // Placeholder
            
            // Folia: Spawneamos el item en el RegionScheduler del bloque
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), mutatedCrop);
            
            // Partículas de congelación
            event.getBlock().getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, 
                    event.getBlock().getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }
}
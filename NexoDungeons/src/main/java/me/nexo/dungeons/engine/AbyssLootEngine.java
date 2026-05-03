package me.nexo.dungeons.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 💎 NexoDungeons - Motor de Botín Adaptativo
 * Centraliza la economía del Abismo multiplicando recompensas y desbloqueando
 * umbrales de rareza basándose en el Gear Score de la instancia.
 */
@Singleton
public class AbyssLootEngine implements Listener {

    private final AbyssScalingEngine scalingEngine;

    @Inject
    public AbyssLootEngine(AbyssScalingEngine scalingEngine) {
        this.scalingEngine = scalingEngine;
    }

    // Usamos EventPriority.HIGH para modificar los drops ANTES de que otros plugins los lean
    @EventHandler(priority = EventPriority.HIGH)
    public void onAbyssMobDeath(EntityDeathEvent event) {
        // Ignoramos la muerte de jugadores (Eso lo maneja AbyssDeathListener)
        if (event.getEntity() instanceof Player) return; 

        String worldName = event.getEntity().getWorld().getName().toLowerCase();
        
        // Solo actuamos dentro del Abismo
        if (!worldName.startsWith("dungeon_") && !worldName.startsWith("inst_")) return;

        // 1. Obtenemos el poder matemático de la Party
        int gearScore = scalingEngine.getGearScore(worldName);
        double multiplier = (double) gearScore / 100.0;

        // 2. MULTIPLICADOR DE CANTIDAD (Quantity Scaling)
        if (multiplier > 1.0) {
            for (ItemStack drop : event.getDrops()) {
                if (drop == null || drop.getType() == Material.AIR) continue;
                
                // Multiplicamos la cantidad original de Minecraft
                int newAmount = (int) (drop.getAmount() * multiplier);
                
                // Evitamos que supere el límite del stack (ej. 64)
                drop.setAmount(Math.min(newAmount, drop.getType().getMaxStackSize()));
            }
        }

        // 3. UMBRALES DE CALIDAD (Quality Thresholds)
        injectRareLoot(event, gearScore);
    }

    /**
     * Motor de Ruleta (RNG) para ítems exclusivos basados en el riesgo.
     */
    private void injectRareLoot(EntityDeathEvent event, int gearScore) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 🌟 TIER 1: Jugadores Intermedios (GS > 150)
        if (gearScore >= 150) {
            if (random.nextDouble() <= 0.10) { // 10% de probabilidad
                // TODO: Conectar con NexoItems para dropear Esencias o Materiales Custom
                event.getDrops().add(new ItemStack(Material.EMERALD, 1));
            }
        }

        // 🌟 TIER 2: Jugadores Avanzados (GS > 250)
        if (gearScore >= 250) {
            if (random.nextDouble() <= 0.05) { // 5% de probabilidad
                // Botín altamente valioso
                event.getDrops().add(new ItemStack(Material.DIAMOND, 1));
            }
        }

        // 🌟 TIER 3: El End-Game (GS > 400)
        if (gearScore >= 400) {
            if (random.nextDouble() <= 0.01) { // 1% de probabilidad extrema
                // Reliquias, fragmentos de invocación de Leviatanes, etc.
                event.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, 1));
            }
        }
    }
}
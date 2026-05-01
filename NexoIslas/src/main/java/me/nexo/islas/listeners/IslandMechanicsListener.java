package me.nexo.islas.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;


import dev.aurelium.auraskills.api.event.skill.XpGainEvent; // Asegúrate de tener AuraSkills en tu build.gradle
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import me.nexo.islas.managers.IslandManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * ⚙️ NexoIslas - Motor Físico de Mejoras (Arquitectura Enterprise)
 * Aplica los multiplicadores de Stat Points en tiempo real (Farming, Mobs, AuraSkills).
 */
@Singleton
public class IslandMechanicsListener implements Listener {

    private final IslandManager islandManager;

    @Inject
    public IslandMechanicsListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    // ==========================================
    // 🌾 MULTIPLICADOR DE CULTIVOS (FARMING DROPS)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropHarvest(BlockBreakEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        // 1. Validamos que esté en una isla ASP
        if (!world.getName().startsWith("island_")) return;

        Material block = event.getBlock().getType();
        if (block != Material.WHEAT && block != Material.POTATOES && block != Material.CARROTS && block != Material.NETHER_WART) {
            return; // No es un cultivo
        }

        IslandProfile profile = islandManager.getIslandAt(event.getBlock().getLocation());
        if (profile == null) return;

        // 2. Obtenemos el multiplicador (Ej: 1.0, 1.2, 1.5, 1.8, 2.0)
        double rateStr = getRate(profile.getFarmingDropLevel());
        if (rateStr <= 1.0) return;

        // 3. Probabilidad de doble drop (Si el rate es 1.5, hay 50% de chance de botín extra)
        double chance = rateStr - 1.0;
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            // Duplicamos el drop nativo de Minecraft
            for (ItemStack drop : event.getBlock().getDrops(player.getInventory().getItemInMainHand())) {
                world.dropItemNaturally(event.getBlock().getLocation(), drop);
            }
        }
    }

    // ==========================================
    // ⚔️ MULTIPLICADOR DE DROPS (MOBS)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        World world = killer.getWorld();
        if (!world.getName().startsWith("island_")) return;

        IslandProfile profile = islandManager.getIslandAt(event.getEntity().getLocation());
        if (profile == null) return;

        double rateStr = getRate(profile.getMobDropLevel());
        if (rateStr <= 1.0) return;

        double chance = rateStr - 1.0;
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            // Duplicamos los ítems que el mob iba a soltar
            for (ItemStack drop : event.getDrops()) {
                world.dropItemNaturally(event.getEntity().getLocation(), drop.clone());
            }
        }
    }

    // ==========================================
    // ✨ MULTIPLICADOR DE EXPERIENCIA (AuraSkills)
    // ==========================================
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAuraSkillsXp(XpGainEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().startsWith("island_")) return;

        IslandProfile profile = islandManager.getIslandAt(player.getLocation());
        if (profile == null) return;

        // Obtenemos el bonus de nuestra matemática (Ej: 1.04x para Nivel 3)
        double bonusMultiplier = profile.getRealXpBonus();

        if (bonusMultiplier > 1.0) {
            // Multiplicamos la XP original de AuraSkills
            double newXp = event.getAmount() * bonusMultiplier;
            event.setAmount(newXp);
        }
    }

    // ==========================================
    // 🧮 TRADUCTOR DE NIVELES A MULTIPLICADORES
    // ==========================================
    private double getRate(int level) {
        return switch(level) {
            case 1 -> 1.0;
            case 2 -> 1.2;
            case 3 -> 1.5;
            case 4 -> 1.8;
            default -> 2.0;
        };
    }
}
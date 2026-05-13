package me.nexo.mechanics.gathering.hazards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.mechanics.NexoMechanics;
import me.nexo.mechanics.gathering.data.Profession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 🧨 Despachador de Peligros (Folia-Ready)
 * Decide qué trampa o mob lanzar basándose en la profesión.
 */
@Singleton
public class HazardDispatcher {

    private final NexoMechanics plugin;
    private final MiniMessage mm = MiniMessage.miniMessage(); // 🌟 Usamos MiniMessage para el texto moderno

    @Inject
    public HazardDispatcher(NexoMechanics plugin) {
        this.plugin = plugin;
    }

    public void dispatch(Profession profession, Player player, Location loc) {
        IHazardEvent hazard = getRandomHazard(profession);
        hazard.execute(player, loc);
    }

    private IHazardEvent getRandomHazard(Profession profession) {
        boolean spawnMob = ThreadLocalRandom.current().nextBoolean();

        return switch (profession) {
            case MINING -> spawnMob
                    ? new MobHazard(plugin, EntityType.SILVERFISH, mm.deserialize("<red>⚠ Parásito de la Falla ⚠</red>"))
                    : new EnvironmentalHazard(plugin, new PotionEffect(PotionEffectType.BLINDNESS, 100, 1), Particle.ASH);
            case WOODCUTTING -> spawnMob
                    ? new MobHazard(plugin, EntityType.VINDICATOR, mm.deserialize("<red>⚠ Espíritu del Bosque ⚠</red>"))
                    : new EnvironmentalHazard(plugin, new PotionEffect(PotionEffectType.SLOWNESS, 100, 2), Particle.ITEM_SLIME);
            case FARMING -> spawnMob
                    ? new MobHazard(plugin, EntityType.CAVE_SPIDER, mm.deserialize("<red>⚠ Plaga Tóxica ⚠</red>"))
                    : new EnvironmentalHazard(plugin, new PotionEffect(PotionEffectType.POISON, 60, 0), Particle.CAMPFIRE_COSY_SMOKE);
        };
    }

    // ==========================================
    // 🦇 IMPLEMENTACIÓN: MOB HAZARD (Premium Loot)
    // ==========================================
    // 🌟 Cambiamos String por Component de Adventure API
    private record MobHazard(NexoMechanics plugin, EntityType type, Component customName) implements IHazardEvent {
        @Override
        public void execute(Player player, Location loc) {
            Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 2);

                Entity mob = loc.getWorld().spawnEntity(loc.clone().add(0.5, 0, 0.5), type);

                // 🌟 Usamos customName en lugar de setCustomName
                mob.customName(customName);
                mob.setCustomNameVisible(true);
            });
        }
    }

    // ==========================================
    // 🧪 IMPLEMENTACIÓN: TRAMPA AMBIENTAL (AoE)
    // ==========================================
    private record EnvironmentalHazard(NexoMechanics plugin, PotionEffect effect, Particle particle) implements IHazardEvent {
        @Override
        public void execute(Player player, Location loc) {
            Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                loc.getWorld().spawnParticle(particle, loc, 100, 3, 2, 3, 0.1);

                // Aplicar efecto a todos en un radio de 4 bloques
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
                    if (e instanceof LivingEntity le) {
                        le.addPotionEffect(effect);
                    }
                }
            });
        }
    }
}
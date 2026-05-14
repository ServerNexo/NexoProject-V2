package me.aeroxis.pvp.mechanics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.NexoFurniture; // 🌟 Solo usaremos Furniture
import com.nexomc.nexo.api.NexoItems; // 🌟 Y el escáner de Ítems
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.pvp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🏛️ AeroxisPvP - Estaciones de Entrenamiento (Arquitectura Enterprise)
 */
@Singleton
public class TrainingStationListener implements Listener {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private final ConfigManager configManager;
    private final CrossplayUtils crossplayUtils;

    private final int MAX_TRAINING_LEVEL = 15;
    private final double XP_PER_BREAK = 10.0;

    @Inject
    public TrainingStationListener(ConfigManager configManager, CrossplayUtils crossplayUtils) {
        this.configManager = configManager;
        this.crossplayUtils = crossplayUtils;
    }

    @EventHandler
    public void onTrainingBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String nexoId = null;

        try {
            // 🌟 1. Intentamos leerlo como Furniture (El Dummy de Pelea)
            var customFurniture = NexoFurniture.furnitureMechanic(block);
            if (customFurniture != null) {
                nexoId = customFurniture.getItemID();
            }
            // 🌟 2. PLAN Z (Para bloques): Leemos el ID del drop sin importar qué tipo de bloque sea
            else {
                for (org.bukkit.inventory.ItemStack drop : block.getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
                    String dropId = NexoItems.idFromItem(drop);
                    if (dropId != null) {
                        nexoId = dropId;
                        break; // Encontramos el ID de Nexo, dejamos de buscar
                    }
                }
            }
        } catch (Exception ignored) {}

        // Si después de todo sigue siendo null, es un bloque normal del mundo (Tierra, Madera, etc.)
        if (nexoId == null) {
            return;
        }

        Skill targetSkill = null;

        // Evaluamos el ID de Nexo
        switch (nexoId) {
            case "dummy_pelea" -> targetSkill = Skills.FIGHTING;
            case "mena_entrenamiento" -> targetSkill = Skills.MINING;
            case "tronco_entrenamiento" -> targetSkill = Skills.FORAGING;
            case "fardo_entrenamiento" -> targetSkill = Skills.FARMING;
            case "libreria_entrenamiento" -> targetSkill = Skills.ENCHANTING;
            case "caldero_entrenamiento" -> targetSkill = Skills.ALCHEMY;
            case "barril_entrenamiento" -> targetSkill = Skills.FISHING;
            default -> { return; }
        }

        // 🛑 Cancelamos la ruptura para que el dummy/bloque sea infinito
        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(id) && (now - cooldowns.get(id)) < 500) {
            return;
        }

        try {
            SkillsUser skillsUser = AuraSkillsApi.get().getUser(id);
            if (skillsUser == null) return;

            // Límite de Entrenamiento
            if (skillsUser.getSkillLevel(targetSkill) >= MAX_TRAINING_LEVEL) {
                if (!cooldowns.containsKey(id) || (now - cooldowns.get(id)) > 3000) {
                    String msg = configManager.getMessages().mensajes().pvp().entrenamientoMaximo()
                            .replace("%nivel%", String.valueOf(MAX_TRAINING_LEVEL));
                    crossplayUtils.sendMessage(player, msg);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    cooldowns.put(id, now);
                }
                return;
            }

            // 🌟 Otorgar XP y Feedback
            skillsUser.addSkillXp(targetSkill, XP_PER_BREAK);
            playTrainingFeedback(player, targetSkill, block.getLocation());

            cooldowns.put(id, now);

        } catch (Exception ignored) {}
    }

    private void playTrainingFeedback(Player player, Skill skill, org.bukkit.Location loc) {
        org.bukkit.Location center = loc.add(0.5, 0.5, 0.5);
        String icon = "[+]";

        if (skill == Skills.FIGHTING) {
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, center, 5);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
            icon = "[⚔]";
        } else if (skill == Skills.MINING) {
            player.getWorld().spawnParticle(Particle.BLOCK, center, 10, Bukkit.createBlockData(Material.COAL_ORE));
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5f, 0.8f);
            icon = "[⛏]";
        } else if (skill == Skills.FORAGING) {
            player.getWorld().spawnParticle(Particle.BLOCK, center, 10, Bukkit.createBlockData(Material.OAK_LOG));
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.5f, 0.8f);
            icon = "[🪓]";
        } else if (skill == Skills.FARMING) {
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 5);
            player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.5f, 1.2f);
            icon = "[🌾]";
        } else if (skill == Skills.ENCHANTING) {
            player.getWorld().spawnParticle(Particle.ENCHANT, center, 15);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 2.0f);
            icon = "[🔮]";
        } else if (skill == Skills.ALCHEMY) {
            player.getWorld().spawnParticle(Particle.WITCH, center, 10);
            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 1.5f);
            icon = "[🧪]";
        } else if (skill == Skills.FISHING) {
            player.getWorld().spawnParticle(Particle.SPLASH, center, 15);
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.5f, 1.5f);
            icon = "[🎣]";
        }

        String actionMsg = configManager.getMessages().mensajes().pvp().entrenamientoXp()
                .replace("%icon%", icon)
                .replace("%xp%", String.valueOf(XP_PER_BREAK));

        crossplayUtils.sendActionBar(player, actionMsg);
    }
}
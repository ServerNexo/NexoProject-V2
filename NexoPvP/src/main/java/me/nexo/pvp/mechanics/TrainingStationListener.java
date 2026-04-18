package me.nexo.pvp.mechanics;

import com.google.inject.Inject;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.pvp.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 🏛️ NexoPvP - Estaciones de Entrenamiento (Arquitectura Enterprise)
 */
public class TrainingStationListener implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final ConfigManager configManager; // 💡 PILAR 2

    // ⚖️ BALANCE: Ahora que romper tarda más que hacer clic, subimos la XP
    private final int MAX_TRAINING_LEVEL = 15;
    private final double XP_PER_BREAK = 10.0;

    // 💉 PILAR 3: Inyección de Dependencias
    @Inject
    public TrainingStationListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onTrainingBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        Skill targetSkill = null;
        Material blockType = block.getType();

        switch (blockType) {
            case TARGET -> targetSkill = Skills.FIGHTING;
            case COAL_ORE -> targetSkill = Skills.MINING;
            case OAK_LOG -> targetSkill = Skills.FORAGING;
            case HAY_BLOCK -> targetSkill = Skills.FARMING;
            case BOOKSHELF -> targetSkill = Skills.ENCHANTING;
            case CAULDRON -> targetSkill = Skills.ALCHEMY;
            case BARREL -> targetSkill = Skills.FISHING;
            default -> { return; }
        }

        // 🛑 MAGIA CORPORATIVA: Dummy Infinito
        event.setCancelled(true);

        if (cooldowns.containsKey(id) && (now - cooldowns.get(id)) < 500) {
            return; // Cooldown anti-lag por instamine
        }

        try {
            SkillsUser skillsUser = AuraSkillsApi.get().getUser(id);
            if (skillsUser == null) return;

            // 🛑 SISTEMA ANTI-AFK INFINITO (Hard-Cap)
            if (skillsUser.getSkillLevel(targetSkill) >= MAX_TRAINING_LEVEL) {
                if (!cooldowns.containsKey(id) || (now - cooldowns.get(id)) > 3000) {
                    // 💡 PILAR 2: Texto Seguro
                    String msg = configManager.getMessages().mensajes().pvp().entrenamientoMaximo()
                            .replace("%nivel%", String.valueOf(MAX_TRAINING_LEVEL));
                    CrossplayUtils.sendMessage(player, msg);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    cooldowns.put(id, now);
                }
                return;
            }

            // 🌟 Otorgar XP y Feedback
            skillsUser.addSkillXp(targetSkill, XP_PER_BREAK);
            playTrainingFeedback(player, blockType, block.getLocation());

            cooldowns.put(id, now);

        } catch (Exception ignored) {}
    }

    private void playTrainingFeedback(Player player, Material blockType, org.bukkit.Location loc) {
        org.bukkit.Location center = loc.add(0.5, 0.5, 0.5);
        String icon = "[+]";

        switch (blockType) {
            case TARGET -> {
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, center, 5);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
                icon = "[⚔]";
            }
            case COAL_ORE -> {
                player.getWorld().spawnParticle(Particle.BLOCK, center, 10, Bukkit.createBlockData(Material.COAL_ORE));
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5f, 0.8f);
                icon = "[⛏]";
            }
            case OAK_LOG -> {
                player.getWorld().spawnParticle(Particle.BLOCK, center, 10, Bukkit.createBlockData(Material.OAK_LOG));
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.5f, 0.8f);
                icon = "[🪓]";
            }
            case HAY_BLOCK -> {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 5);
                player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.5f, 1.2f);
                icon = "[🌾]";
            }
            case BOOKSHELF -> {
                player.getWorld().spawnParticle(Particle.ENCHANT, center, 15);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 2.0f);
                icon = "[🔮]";
            }
            case CAULDRON -> {
                player.getWorld().spawnParticle(Particle.WITCH, center, 10);
                player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 1.5f);
                icon = "[🧪]";
            }
            case BARREL -> {
                player.getWorld().spawnParticle(Particle.SPLASH, center, 15);
                player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.5f, 1.5f);
                icon = "[🎣]";
            }
        }

        // 💡 PILAR 2: ActionBar segura con variables
        String actionMsg = configManager.getMessages().mensajes().pvp().entrenamientoXp()
                .replace("%icon%", icon)
                .replace("%xp%", String.valueOf(XP_PER_BREAK));

        CrossplayUtils.sendActionBar(player, actionMsg);
    }
}
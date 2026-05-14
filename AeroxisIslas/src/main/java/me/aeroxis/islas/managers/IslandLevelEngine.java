package me.aeroxis.islas.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.aeroxis.core.crossplay.CrossplayUtils;
import me.aeroxis.islas.AeroxisIslas;
import me.aeroxis.islas.data.IslandProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 📈 AeroxisIslas - Motor de Niveles y Progresión
 * Calcula la XP basada en el aporte de miembros, integra EMF y Jefes Custom.
 */
@Singleton
public class IslandLevelEngine {

    private final AeroxisIslas plugin;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public IslandLevelEngine(AeroxisIslas plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    // ==========================================
    // 📊 LECTURA DINÁMICA DE CONFIG.YML
    // ==========================================
    public double getRequiredXp(int level) {
        double base = plugin.getConfig().getDouble("level-engine.base-xp", 1000.0);
        double mult = plugin.getConfig().getDouble("level-engine.multiplier", 1.15);
        return base * Math.pow(mult, level - 1);
    }

    public double getBlockXp(String materialName) {
        return plugin.getConfig().getDouble("level-engine.sources.blocks." + materialName, 0.0);
    }

    // 🌟 COMPATIBILIDAD CON JEFES DE NEXOCORE Y MOBS VANILLA
    public double getMobXp(Entity entity) {
        var pdc = entity.getPersistentDataContainer();
        NamespacedKey bossKey = new NamespacedKey("nexocore", "boss_id");

        if (pdc.has(bossKey, PersistentDataType.STRING)) {
            String customBossId = pdc.get(bossKey, PersistentDataType.STRING);
            return plugin.getConfig().getDouble("level-engine.sources.mobs." + customBossId, 0.0);
        }

        return plugin.getConfig().getDouble("level-engine.sources.mobs." + entity.getType().name(), 0.0);
    }

    public double getMobXp(String entityName) {
        return plugin.getConfig().getDouble("level-engine.sources.mobs." + entityName, 0.0);
    }

    // 🎣 COMPATIBILIDAD NATIVA CON EVEN MORE FISH
    public double getFishXp(ItemStack fishItem) {
        if (fishItem == null || !fishItem.hasItemMeta()) return 0.0;

        NamespacedKey emfRarityKey = new NamespacedKey("evenmorefish", "emf-fish-rarity");
        String rarity = fishItem.getItemMeta().getPersistentDataContainer().get(emfRarityKey, PersistentDataType.STRING);

        if (rarity != null) {
            return plugin.getConfig().getDouble("level-engine.sources.emf-rarities." + rarity, 5.0);
        }
        return 0.0;
    }

    // ==========================================
    // 🧠 MOTOR DE CÁLCULO (XP POR JUGADOR)
    // ==========================================
    public void addXp(IslandProfile profile, UUID playerId, double amount) {
        if (amount <= 0) return;

        profile.addPlayerXp(playerId, amount); // Guardado en el mapa concurrente

        // Feedback de Sonido Sutil para saber que sumaste XP (Opcional)
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 2.0f);
        }

        recalculateLevel(profile);
    }

    public void recalculateLevel(IslandProfile profile) {
        double totalXp = profile.getTotalXp();
        int calculatedLevel = 1;

        double xpNeededForNext = getRequiredXp(calculatedLevel);
        while (totalXp >= xpNeededForNext) {
            totalXp -= xpNeededForNext;
            calculatedLevel++;
            xpNeededForNext = getRequiredXp(calculatedLevel);
        }

        int currentLevel = profile.getLevel();
        if (calculatedLevel > currentLevel) {
            profile.setLevel(calculatedLevel);
            announceLevelUp(profile);
        } else if (calculatedLevel < currentLevel) {
            profile.setLevel(calculatedLevel);
        }
    }

    // ==========================================
    // 🎉 EFECTOS VISUALES Y NOTIFICACIONES
    // ==========================================
    private void announceLevelUp(IslandProfile profile) {
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500));

        // 🌟 FIX CRÍTICO: Añadimos al DUEÑO a la lista de notificaciones
        List<UUID> todosLosHabitantes = new ArrayList<>(profile.getMembers().keySet());
        todosLosHabitantes.add(profile.getOwnerId()); // ¡Ahora el dueño sí se entera!

        for (UUID memberId : todosLosHabitantes) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null && p.isOnline()) {
                crossplayUtils.sendMessage(p, "");
                crossplayUtils.sendMessage(p, "&#FFD700🌟 <bold>¡TU ISLA HA SUBIDO DE NIVEL!</bold> 🌟");
                crossplayUtils.sendMessage(p, "&#55FF55Nivel Actual: &#FFFFFF" + profile.getLevel());
                crossplayUtils.sendMessage(p, "");
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                Component mainTitle = crossplayUtils.parseCrossplay(p, "&#FFD700<bold>¡NIVEL " + profile.getLevel() + "!</bold>");
                Component subTitle = crossplayUtils.parseCrossplay(p, "&#E6CCFFTu isla se hace más fuerte");

                Title title = Title.title(mainTitle, subTitle, times);
                p.showTitle(title);
            }
        }
    }
}
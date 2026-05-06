package me.nexo.islas.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
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
import java.util.UUID;

/**
 * 📈 NexoIslas - Motor de Niveles y Progresión
 * Calcula la XP basada en el aporte de miembros, integra EMF y Jefes Custom.
 */
@Singleton
public class IslandLevelEngine {

    private final NexoIslas plugin;
    private final CrossplayUtils crossplayUtils;

    @Inject
    public IslandLevelEngine(NexoIslas plugin, CrossplayUtils crossplayUtils) {
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

    // 🌟 COMPATIBILIDAD CON JEFES DE NEXOCORE Y MOBS VANILLA (Para Eventos en Vivo)
    public double getMobXp(Entity entity) {
        var pdc = entity.getPersistentDataContainer();
        NamespacedKey bossKey = new NamespacedKey("nexocore", "boss_id");

        // 1. ¿Es un Jefe Custom de NexoCore?
        if (pdc.has(bossKey, PersistentDataType.STRING)) {
            String customBossId = pdc.get(bossKey, PersistentDataType.STRING);
            return plugin.getConfig().getDouble("level-engine.sources.mobs." + customBossId, 0.0);
        }

        // 2. Si no lo es, leemos la XP de su tipo Vanilla
        return plugin.getConfig().getDouble("level-engine.sources.mobs." + entity.getType().name(), 0.0);
    }

    // 🌟 AÑADIDO: BÚSQUEDA DE MOBS POR STRING (Necesario para que el Omni-Minion lea la XP offline)
    public double getMobXp(String entityName) {
        return plugin.getConfig().getDouble("level-engine.sources.mobs." + entityName, 0.0);
    }

    // 🎣 COMPATIBILIDAD NATIVA CON EVEN MORE FISH (Rarezas)
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
    /**
     * Añade la XP a la "cuenta personal" del jugador en la isla y recalcula todo.
     */
    public void addXp(IslandProfile profile, UUID playerId, double amount) {
        if (amount <= 0) return;

        profile.addPlayerXp(playerId, amount); // Guardado en el mapa concurrente
        recalculateLevel(profile);
    }

    /**
     * Recalcula el nivel desde cero sumando el aporte de TODOS los miembros.
     * Esto permite que el nivel baje si un jugador es expulsado.
     */
    public void recalculateLevel(IslandProfile profile) {
        double totalXp = profile.getTotalXp();
        int calculatedLevel = 1;

        // Escala nivel por nivel consumiendo la XP total
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
            // Un miembro con mucha XP abandonó la isla
            profile.setLevel(calculatedLevel);
        }
    }

    // ==========================================
    // 🎉 EFECTOS VISUALES Y NOTIFICACIONES
    // ==========================================
    private void announceLevelUp(IslandProfile profile) {
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500));

        for (UUID memberId : profile.getMembers().keySet()) {
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
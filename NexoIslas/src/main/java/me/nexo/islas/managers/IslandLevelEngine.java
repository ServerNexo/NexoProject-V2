package me.nexo.islas.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.nexo.core.crossplay.CrossplayUtils;
import me.nexo.islas.NexoIslas;
import me.nexo.islas.data.IslandProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

@Singleton
public class IslandLevelEngine {

    private final NexoIslas plugin;
    private final CrossplayUtils crossplayUtils;

    // Configuración RPG
    private final double BASE_XP = 1000.0;
    private final double MULTIPLIER = 1.15; // Cada nivel pide 15% más de XP que el anterior

    @Inject
    public IslandLevelEngine(NexoIslas plugin, CrossplayUtils crossplayUtils) {
        this.plugin = plugin;
        this.crossplayUtils = crossplayUtils;
    }

    /**
     * Calcula la XP requerida para alcanzar el siguiente nivel.
     * Nivel 1 -> 2: 1000 XP
     * Nivel 2 -> 3: 1150 XP
     * Nivel 10 -> 11: 3517 XP
     */
    public double getRequiredXp(int currentLevel) {
        return BASE_XP * Math.pow(MULTIPLIER, currentLevel - 1);
    }

    /**
     * Inyecta XP a la isla y verifica recursivamente si subió de nivel.
     */
    public void addXp(IslandProfile profile, double amount) {
        double currentXp = profile.getXp() + amount;
        int currentLevel = profile.getLevel();
        boolean leveledUp = false;

        while (currentXp >= getRequiredXp(currentLevel)) {
            currentXp -= getRequiredXp(currentLevel); // Restamos la XP consumida
            currentLevel++;
            leveledUp = true;
        }

        profile.setXp(currentXp);

        if (leveledUp) {
            profile.setLevel(currentLevel);
            announceLevelUp(profile);
            // Aquí en un futuro podrías guardar inmediatamente en la BD
        }
    }

    private void announceLevelUp(IslandProfile profile) {
        // Tiempos de Kyori Adventure: FadeIn (500ms), Stay (3000ms), FadeOut (500ms)
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500));

        // Notificamos a todos los miembros de la isla que estén conectados
        for (UUID memberId : profile.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null && p.isOnline()) {
                crossplayUtils.sendMessage(p, "");
                crossplayUtils.sendMessage(p, "&#FFD700🌟 <bold>¡TU ISLA HA SUBIDO DE NIVEL!</bold> 🌟");
                crossplayUtils.sendMessage(p, "&#55FF55Nivel Actual: &#FFFFFF" + profile.getLevel());
                crossplayUtils.sendMessage(p, "");
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                // 🌟 FIX: Título moderno de Paper/Kyori Adventure con soporte Hexadecimal
                Component mainTitle = crossplayUtils.parseCrossplay(p, "&#FFD700<bold>¡NIVEL " + profile.getLevel() + "!</bold>");
                Component subTitle = crossplayUtils.parseCrossplay(p, "&#E6CCFFTu isla se hace más fuerte");

                Title title = Title.title(mainTitle, subTitle, times);
                p.showTitle(title);
            }
        }
    }
}
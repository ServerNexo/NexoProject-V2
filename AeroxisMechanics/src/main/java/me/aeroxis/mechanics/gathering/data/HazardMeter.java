package me.aeroxis.mechanics.gathering.data;

import me.aeroxis.mechanics.gathering.managers.SanctuaryManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/**
 * ⚠️ Medidor de Tensión Individual
 * Ticker activo de decadencia de estrés.
 */
public class HazardMeter {

    private final BossBar bossBar;
    private double currentTension = 0.0;
    private long lastActionTime = 0;

    private static final long DECAY_DELAY_MS = 10000; // 10s para relajarse
    private static final double DECAY_RATE_PER_SEC = 5.0; // Pierde 5% por segundo

    public HazardMeter(String title) {
        this.bossBar = BossBar.bossBar(
                MiniMessage.miniMessage().deserialize("<bold><white>⚠ " + title + " ⚠</white></bold>"),
                0.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );
    }

    public void addTension(double amount, Player player, SanctuaryManager sanctuary) {
        if (sanctuary.isProtected(player)) return;

        currentTension = Math.min(100.0, currentTension + amount);
        this.lastActionTime = System.currentTimeMillis(); // Reiniciamos el reloj de inactividad
        updateBossBar(player);
    }

    public void resetTension(Player player) {
        this.currentTension = 0.0;
        updateBossBar(player);
    }

    // 🌟 NUEVO: Este método lo llamará el Manager cada segundo en segundo plano
    public void tickDecay(Player player) {
        if (currentTension <= 0) return;

        long now = System.currentTimeMillis();
        if (now - lastActionTime > DECAY_DELAY_MS) {
            currentTension = Math.max(0.0, currentTension - DECAY_RATE_PER_SEC);
            updateBossBar(player);
        }
    }

    public boolean isOverloaded() {
        return currentTension >= 100.0;
    }

    public double getTension() {
        return currentTension;
    }

    private void updateBossBar(Player player) {
        // 🌟 FIX: Si la tensión baja a 0 (o explota y se resetea), ocultamos la barra
        if (currentTension <= 0) {
            hide(player);
            return;
        }

        float progress = (float) (currentTension / 100.0);
        bossBar.progress(progress);

        if (progress < 0.5f) {
            bossBar.color(BossBar.Color.GREEN);
            bossBar.overlay(BossBar.Overlay.PROGRESS);
        } else if (progress < 0.8f) {
            bossBar.color(BossBar.Color.YELLOW);
            bossBar.overlay(BossBar.Overlay.PROGRESS);
        } else {
            bossBar.color(BossBar.Color.RED);
            bossBar.overlay(BossBar.Overlay.NOTCHED_10);
        }

        player.showBossBar(bossBar);
    }

    public void hide(Player player) {
        player.hideBossBar(bossBar);
    }
}
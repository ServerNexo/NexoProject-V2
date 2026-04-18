package me.nexo.colecciones.slayers;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 📚 NexoColecciones - Contrato Slayer Activo (POJO)
 * Nota: Objeto en RAM en tiempo real. Thread-Safe. No requiere Inyección.
 */
public class ActiveSlayer {

    private final UUID playerId;
    private final SlayerManager.SlayerTemplate template;

    // 🌟 FIX: Atómicos y Volátiles para evitar Crashes en muertes simultáneas (Multihilo)
    private final AtomicInteger currentKills;
    private volatile boolean bossSpawned;
    private BossBar bossBar;

    public ActiveSlayer(Player player, SlayerManager.SlayerTemplate template) {
        this.playerId = player.getUniqueId();
        this.template = template;
        this.currentKills = new AtomicInteger(0);
        this.bossSpawned = false;
        this.bossBar = null;
    }

    public void addKill() {
        if (!bossSpawned && currentKills.get() < template.requiredKills()) {
            this.currentKills.incrementAndGet(); // 🌟 FIX: Incremento atómico y seguro
        }
    }

    // 🌟 GETTERS Y SETTERS ENTERPRISE
    public UUID getPlayerId() { return playerId; }

    public SlayerManager.SlayerTemplate getTemplate() { return template; }

    public int getKills() { return currentKills.get(); } // Retorna el valor nativo

    public String getBossName() { return template.bossName(); }

    public boolean isBossSpawned() { return bossSpawned; }
    public void setBossSpawned(boolean bossSpawned) { this.bossSpawned = bossSpawned; }

    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }
}
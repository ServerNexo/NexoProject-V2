package me.nexo.core.bosses;

import me.nexo.core.crossplay.CrossplayUtils; // 🌟 NUEVO IMPORT
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 👑 NexoGlobalBoss - Hereda la IA base y añade Ránking de Daño y Botín.
 */
public abstract class NexoGlobalBoss extends NexoBoss {

    private static final Map<UUID, NexoGlobalBoss> ACTIVE_BOSSES = new ConcurrentHashMap<>();

    public static NexoGlobalBoss getActiveBoss(UUID entityId) {
        return ACTIVE_BOSSES.get(entityId);
    }

    protected final DamageTracker damageTracker;
    protected final CrossplayUtils crossplayUtils; // 🌟 GUARDAMOS EL UTIL
    private final String displayName;

    // 🌟 FIX: Añadimos CrossplayUtils al constructor
    public NexoGlobalBoss(JavaPlugin plugin, CrossplayUtils crossplayUtils, String displayName) {
        super(plugin);
        this.crossplayUtils = crossplayUtils;
        this.displayName = displayName;
        this.damageTracker = new DamageTracker();
    }

    public DamageTracker getTracker() {
        return damageTracker;
    }

    @Override
    public void spawn(Location location, int groupGearScore, String internalBossName) {
        super.spawn(location, groupGearScore, internalBossName);
        if (this.entity != null) {
            ACTIVE_BOSSES.put(this.entity.getUniqueId(), this);
        }
    }

    @Override
    protected void onDeath() {
        if (this.entity != null) {
            ACTIVE_BOSSES.remove(this.entity.getUniqueId());
        }

        this.entity.getWorld().playSound(this.entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);

        Thread.startVirtualThread(() -> {
            List<Map.Entry<UUID, Double>> topDamagers = damageTracker.getTopDamagers(3);

            Bukkit.getScheduler().runTask(plugin, () -> {
                // 🌟 FIX: Usamos crossplayUtils.broadcastMessage en lugar del método nativo
                crossplayUtils.broadcastMessage("&#555555--------------------------------");
                crossplayUtils.broadcastMessage("&#FF3366💀 <bold>" + displayName.toUpperCase() + " HA SIDO DERROTADO!</bold>");
                crossplayUtils.broadcastMessage(" ");

                String[] medallas = {"&#FFD700🥇", "&#C0C0C0🥈", "&#CD7F32🥉"};

                for (int i = 0; i < topDamagers.size(); i++) {
                    UUID pUuid = topDamagers.get(i).getKey();
                    double dmg = topDamagers.get(i).getValue();
                    double pct = damageTracker.getPercentage(pUuid);

                    Player p = Bukkit.getPlayer(pUuid);
                    String pName = (p != null) ? p.getName() : "Desconectado";

                    crossplayUtils.broadcastMessage(medallas[i] + " &#E6CCFF" + pName + " &#555555- &#FF5555⚔ " + String.format("%.0f", dmg) + " (" + String.format("%.1f", pct) + "%)");

                    if (p != null) {
                        entregarRecompensaTop(p, i + 1);
                    }
                }

                for (UUID pUuid : damageTracker.getAllParticipants()) {
                    if (damageTracker.getPercentage(pUuid) >= 5.0) {
                        Player p = Bukkit.getPlayer(pUuid);
                        if (p != null) {
                            entregarRecompensaParticipacion(p);
                        }
                    }
                }

                crossplayUtils.broadcastMessage("&#555555--------------------------------");
            });
        });
    }

    protected abstract void entregarRecompensaTop(Player player, int rank);
    protected abstract void entregarRecompensaParticipacion(Player player);
}
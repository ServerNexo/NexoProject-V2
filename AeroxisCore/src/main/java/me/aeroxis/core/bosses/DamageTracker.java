package me.aeroxis.core.bosses;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 📊 Rastreador de Daño para Jefes Globales (Thread-Safe)
 */
public class DamageTracker {

    private final Map<UUID, Double> damageMap = new ConcurrentHashMap<>();
    private double totalDamage = 0.0;
    private UUID summonerId = null;

    public void setSummoner(UUID uuid) {
        this.summonerId = uuid;
    }

    public UUID getSummoner() {
        return summonerId;
    }

    public void addDamage(UUID playerUuid, double damage) {
        damageMap.merge(playerUuid, damage, Double::sum);
        totalDamage += damage;
    }

    public double getDamage(UUID playerUuid) {
        return damageMap.getOrDefault(playerUuid, 0.0);
    }

    public double getPercentage(UUID playerUuid) {
        if (totalDamage == 0) return 0.0;
        return (getDamage(playerUuid) / totalDamage) * 100.0;
    }

    /**
     * Retorna una lista ordenada de los jugadores que hicieron más daño.
     */
    public List<Map.Entry<UUID, Double>> getTopDamagers(int limit) {
        return damageMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Set<UUID> getAllParticipants() {
        return damageMap.keySet();
    }
}
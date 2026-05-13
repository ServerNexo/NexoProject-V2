package me.nexo.mechanics.gathering.data;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📊 Perfil de recolección en memoria.
 * Protegido para lecturas/escrituras asíncronas masivas (Folia-Ready).
 */
public class GatheringProfile {

    private final UUID uuid;
    private final Map<String, Integer> blockProgress = new ConcurrentHashMap<>();
    private final Map<Profession, HazardMeter> hazardMeters = new ConcurrentHashMap<>();

    public GatheringProfile(UUID uuid) {
        this.uuid = uuid;
        for (Profession prof : Profession.values()) {
            String display = switch(prof) {
                case MINING -> "Tensión Minera";
                case WOODCUTTING -> "Tensión Forestal";
                case FARMING -> "Tensión Agrícola";
            };
            hazardMeters.put(prof, new HazardMeter(display));
        }
    }

    public void addProgress(String blockId, int amount) {
        blockProgress.merge(blockId, amount, Integer::sum);
    }

    public int getProgress(String blockId) {
        return blockProgress.getOrDefault(blockId, 0);
    }

    public HazardMeter getMeter(Profession profession) {
        return hazardMeters.get(profession);
    }

    // 🌟 Actualiza la decadencia de todas las barras a la vez
    public void tickAllMeters(Player player) {
        for (HazardMeter meter : hazardMeters.values()) {
            meter.tickDecay(player);
        }
    }

    public void hideAllMeters(Player player) {
        for (HazardMeter meter : hazardMeters.values()) {
            meter.hide(player);
        }
    }

    // ==========================================
    // 💾 MÉTODOS PARA BASE DE DATOS
    // ==========================================
    public Map<String, Integer> getAllProgress() {
        return blockProgress;
    }

    public void setAllProgress(Map<String, Integer> progress) {
        this.blockProgress.putAll(progress);
    }
}